# Feature: Client-side request ordering

## Goal
Guarantee that fetch requests issued by the client reach the server in the order they were issued, preventing racing requests (e.g. two rapid `issueSize` updates) from corrupting server state. The client enforces ordering by holding later requests until any earlier requests on the same URL resource — or any prefix of it — have settled. Idempotent requests coalesce so only the latest queued one fires. Failures cascade to anything waiting on the failed request, except for AbortErrors (which do not).

## Key Decisions

- **Resource identity = URL path**: Requests block each other if one path is a prefix of (or equal to) the other, comparing by `/`-separated segments. Example: `/security/CSP1/issueDate` is blocked by `/security/CSP1`, and `/security/CSP1` is blocked by `/security/CSP1/issueDate` (both directions — parent ↔ descendant always conflict). Siblings (`/security/CSP1/issueDate` vs `/security/CSP1/issueSize`) do not block.
- **Method is not part of the key**: GET/PUT/POST/DELETE on the same URL all participate in ordering. This is intentional — reads must observe prior writes, and different verbs on the same resource must serialize.
- **Idempotency inferred from HTTP method**: POST is non-idempotent; GET/PUT/DELETE are idempotent. An explicit `X-Idempotent: true|false` request header overrides the inference. No call site in the codebase currently needs the override, but `FetchBuilder` still exposes a `withIdempotent(boolean)` method so the override is available when a future case arises. Method inference is the default; if `withIdempotent` is not called, no header is emitted and the client-side JS falls back to the method-based rule. This aligns with HTTP conventions and removes the "remember to set the flag" footgun for typical call sites.
- **Idempotent = latest wins**: When an idempotent request arrives on a path that already has a queued (not yet running) request, the queued one is dropped (resolved with a synthetic 204) and replaced. Running requests are never preempted.
- **Error cascade**: If a blocker rejects, every request waiting on it rejects with the same error. Rationale: a failed predecessor usually means server state is suspect, so downstream requests on the same resource shouldn't proceed.
- **Aborts do not cascade**: AbortErrors are intentional and say nothing about server state, so they are filtered out of the cascade logic. Dependents proceed as if the aborted request settled normally.
- **Queue structure `Map<path, Entry[]>`**: Multiple entries per path are needed so non-idempotent requests on the same path actually queue (a single-slot map would let the newer one overwrite the running predecessor). Idempotent replacement targets the last entry in the list if it is queued.
- **Scope the wrapper to DataStar requests**: The monkey-patch activates only when the `Datastar-Request` header is present on the fetch. Other fetches (if any are ever added) pass through untouched. Matches the existing tabId guard on Shell:71.
- **Single combined monkey-patch file `fetch-monkeypatch.js`**: Folds the existing X-tabID wrapper from `Shell.java:67-76` and the new ordering logic into one `window.fetch` replacement. `tabId` is handed in via an inline `window.__tabID = '...'` script tag before the file is loaded.
- **Loaded before DataStar**: Script tag must come before `datastar1.0.0.RC8.js` so that `window.fetch` is wrapped before DataStar captures it in `Nn` via `qe = d || window.fetch`.
- **Routes become resource-oriented**: Current routes are reshaped so resource identity (cusip, trade id) sits in the URL and verbs express operation semantics correctly (PUT for idempotent updates, POST for creations, DELETE for removals). Without this, path-based ordering can't distinguish resources and method-based idempotency gives the wrong default.

## Route refactor

### SecuritiesScreen

| Current | New |
|---------|-----|
| `POST /security/input/{field}` | **PUT** `/security/{cusip}/edit/{field}` |
| `POST /security/save` | **PUT** `/security/{cusip}` |
| `POST /security/edit` | `POST /security/{cusip}/edit` |
| `POST /security/cancel` | `DELETE /security/{cusip}/edit` |
| `POST /security/resetSchedule` (bulk update) | **PUT** `/security/{cusip}/edit/resetSchedule` |
| `PUT /security/resetSchedule` (add entry) | **POST** `/security/{cusip}/edit/resetSchedule` |
| `DELETE /security/resetSchedule/{id}` | `DELETE /security/{cusip}/edit/resetSchedule/{id}` |
| same for callSchedule / putSchedule / sinkingFundSchedule | `.../{cusip}/edit/<schedule>[/{id}]` with the same POST/PUT swap |
| `POST /security/updates` | **GET** `/security?updates` |
| `GET /security/{cusip}/{section}` | **GET** `/security/{cusip}?section={section}` |

Ordering rationale: field and schedule operations live under `/security/{cusip}/edit/...`. Edit-start (`POST /{cusip}/edit`) and cancel (`DELETE /{cusip}/edit`) share the `/{cusip}/edit` path, which is prefix-parent of all field/schedule paths → both wait for any in-flight field/schedule work. Section render (`GET /{cusip}`) and save (`PUT /{cusip}`) share the `/{cusip}` path, which is the prefix of `/{cusip}/edit` and everything below → both wait for everything. Field updates on the same path serialize via the queue and coalesce (PUT is idempotent under method inference). Fields on different paths (different field names) run in parallel. Rapid Edit clicks are POST so they queue rather than coalesce, but this is harmless — the server handler is idempotent (no-op if already editing), and the UI swaps the button anyway.

Note the POST↔PUT swap on the schedule endpoints: the original code had POST=bulk-update / PUT=add-entry, which is backwards from HTTP convention. The new scheme (PUT=bulk-update / POST=add-entry) matches convention AND gives correct idempotency inference for free.

The cusip must be available on the client at render time so actions can embed it; `SecuritiesScreen` already knows it via `getCurrentCusip()`. The `GET /security/{cusip}` handler reads `section` as an optional query parameter (defaulting to "general"). The root `GET /security` redirect targets `/security/nocusip` (no query param needed).

### TradesScreen

| Current | New |
|---------|-----|
| `POST /trades/modify/{id}` (opens modify popup) | unchanged — screen action, popup opener |
| `GET /trades/delete/{id}` (opens delete confirmation) | unchanged |
| `POST /trades/delete/{id}` (executes delete) | `DELETE /trades/{id}` |
| `POST /trades/updates` | **GET** `/trades?updates` |
| `GET /trades` | unchanged |

`Trade` becomes a real resource under `/trades/{id}`. The confirm-opener `GET /trades/delete/{id}` and the actual-delete `DELETE /trades/{id}` now live on different paths, which is fine — the confirm just opens a popup, and the user clicking "Delete" in the popup fires the DELETE. No race.

### TradeTicketPopup

| Current | New |
|---------|-----|
| `POST /ticket/buy` (opens buy popup) | unchanged |
| `POST /ticket/sell` (opens sell popup) | unchanged |
| `POST /ticket/cancel` (closes popup) | `DELETE /ticket` |
| `POST /ticket/input` (updates draft fields) | **PUT** `/ticket` |
| `POST /ticket/book` (commits draft) | split into **POST** `/trades` (new trade) and **PUT** `/trades/{id}` (modify existing) |

Rationale:
- Draft ticket state is a single resource at `/ticket` — field updates are a bulk idempotent replace (PUT), closing is a DELETE. `buy`/`sell` stay as POST sub-actions since they create a new draft (non-idempotent from the client's perspective: two rapid Buy clicks should both open fresh drafts — though in practice the popup being open suppresses further opens).
- Booking splits by operation: POST `/trades` creates a new trade from the draft, PUT `/trades/{id}` replaces an existing trade. The popup's render determines which URL the "Book" button targets based on whether the draft has an existing trade id.
- `/ticket/input` (now PUT `/ticket`) is on a different path than `/trades/*`, so the client-side ordering does NOT block booking on in-flight input updates. This is intentional and relies on the existing UI-level guard: the Book button is disabled while `/ticket` input is in flight, so the user cannot fire Book before the last input settles. Previously agreed "good enough" in earlier discussion.

### DataGrid

| Current | New |
|---------|-----|
| `GET /grid/column-picker` | unchanged |
| `POST /grid/column-picker` (apply) | **PUT** `/grid/column-picker` |
| `DELETE /grid/column-picker` (cancel) | unchanged |
| `POST /grid/sort/{columnName}` | **PUT** `/grid/sort/{columnName}` |
| `POST /grid/filter` | **PUT** `/grid/filter` |
| `POST /grid/viewport` | **PUT** `/grid/viewport` |

All grid operations are idempotent (applying the same sort/filter/viewport twice yields the same result), so they become PUTs. Viewport in particular benefits: rapid scroll events will coalesce via idempotent replacement, capping the queue at one running + one pending.

### PositionsScreen

| Current | New |
|---------|-----|
| `POST /positions/updates` | **GET** `/positions?updates` |
| `GET /positions` | unchanged |

## Implementation Steps

1. **Create `fetch-monkeypatch.js`** — new file `src/main/resources/static/fetch-monkeypatch.js`.
   - Wraps `window.fetch`. Only intercepts requests with `Datastar-Request` header; pass-through otherwise.
   - Adds `X-tabID: window.__tabID` to intercepted requests (ports the existing Shell:67 logic).
   - Idempotency determination: read `X-Idempotent` from headers (plain-object or Headers-instance form). If `'true'` or `'false'`, use that. Otherwise return `method !== 'POST'`. Method comes from `init.method` uppercased, defaulting to `'GET'`.
   - Maintains `const queue = new Map()` where values are `Entry[]`. `Entry = { promise, resolve, reject, state }`.
   - `blocks(a, b)` splits both paths on `/`, filters empty segments, returns true if one segment list is a prefix of the other (or equal).
   - Idempotent replacement: if the path's list has a last entry in `queued` state, resolve it with `new Response(null, { status: 204, statusText: 'Superseded' })`, pop it from the list, delete the list if empty.
   - Append the new entry to the path's list (creating the list if missing).
   - Run logic (async IIFE):
     - Build blockers by iterating `queue.entries()`: for each path where `blocks(path, currentPath)` is true, include every item in that list except `entry` itself.
     - Wrap each blocker with `.catch(err => { if (err?.name === 'AbortError') return; throw err; })` so aborts don't cascade.
     - `await Promise.all(wrapped)`.
     - Re-check that `entry` is still in its list (idempotent replacement could have removed it while we waited) — if not, return without firing.
     - Check `init.signal?.aborted` — if set, reject with an AbortError and return without calling originalFetch.
     - Set `state = 'running'`, call `originalFetch(input, init)`, resolve with the response. Catch and reject on any error.
     - `finally`: splice entry out of its list, delete the list entry from the map if empty.

2. **Inject `window.__tabID` and load the monkey-patch** — modify `src/main/java/com/bookie/screens/Shell.java`.
   - Replace the inline `<script>` block (lines 67-76) with:
     - `<script>window.__tabID = '${tabId}';</script>`
     - `<script src="/fetch-monkeypatch.js"></script>` — **before** the existing DataStar script tag.

3. **Add `withIdempotent(boolean)` to `FetchBuilder`** — modify `src/main/java/com/bookie/infra/FetchBuilder.java`.
   - New nullable field `private Boolean idempotent = null;` (null = method-based inference on the client).
   - New method `public FetchBuilder withIdempotent(boolean value)` that sets the field.
   - In `buildOptions()`: when `idempotent != null`, append `headers: {'X-Idempotent': 'true'}` or `'false'` to the DataStar options object. When null, emit nothing (client infers from method).
   - No call sites use this yet; it's an escape hatch for future cases where method verb and desired idempotency diverge.

4. **Refactor SecuritiesScreen routes** — modify `src/main/java/com/bookie/screens/securities/SecuritiesScreen.java`.
   - Rename the URL prefix from `/securities` to `/security` (update the `RoutePrefix` constant). The Java package `com.bookie.screens.securities` and class `SecuritiesScreen` stay — only the HTTP route changes.
   - Update `setupRoutes` per the SecuritiesScreen table:
     - `GET /{cusip}` — replaces `GET /{cusip}/{section}`. Read section from `request.param("section")`, default to "general".
     - `POST /{cusip}/edit` — start edit (was `POST /edit`). Handler must be idempotent — no-op if already editing.
     - `DELETE /{cusip}/edit` — cancel edit (was `POST /cancel`).
     - `PUT /{cusip}` — save (was `POST /save`).
     - `PUT /{cusip}/edit/{field}` — field updates (was `POST /input/{field}`).
     - Schedules: `PUT /{cusip}/edit/<schedule>` (bulk), `POST /{cusip}/edit/<schedule>` (add), `DELETE /{cusip}/edit/<schedule>/{id}` (delete). Note the POST↔PUT swap from the current code.
   - Update the root redirect from `/security/nocusip/general` to `/security/nocusip`.
   - Update handler signatures to read `{cusip}` from the path and verify it matches `currentBond.getCusip()` (or `editingBond`); mismatch → 409.
   - Update client call sites that build these URLs: `renderEditActions` (edit uses `X.post("/security/" + cusip + "/edit")`, cancel uses `X.delete(...)`, save uses `X.put("/security/" + cusip)`), `GeneralSection` / `IncomeSection` / `RedemptionSection` `data-on:change` action (now `X.put` to `/security/{cusip}/edit/{field}`), schedule add/delete actions (now under `/edit/`), and section `link()` calls (now `security/{cusip}?section={section}`). All call sites have the cusip in scope via `getActiveBond().getCusip()`.

5. **Refactor TradesScreen delete route** — modify `src/main/java/com/bookie/screens/TradesScreen.java`.
   - Rename `POST /delete/{id}` handler route to `DELETE /{id}` (the handler body is unchanged).
   - Update the "Delete" button's action URL in the delete-confirmation popup to use `X.delete("/trades/" + id)`.

6. **Refactor TradeTicketPopup routes** — modify `src/main/java/com/bookie/screens/TradeTicketPopup.java`.
   - Replace `POST /ticket/cancel` with `DELETE /ticket`.
   - Replace `POST /ticket/input` with `PUT /ticket`.
   - Remove the `POST /ticket/book` route.
   - Add new routes at the trades-screen level (in `TradesScreen.setupRoutes`): `POST /trades` (book new trade) and `PUT /trades/{id}` (modify existing trade). Both delegate into a `TradesScreen.onBookTrade` / `onModifyTrade` method that wraps the existing `tradeRepository.bookTrade(trade)` call; or inject the ticket popup into TradesScreen so it can reuse the existing `onBookTrade` logic. The split is purely about the URL/id — the repository call is the same.
   - Update the popup's render to emit `POST /trades` or `PUT /trades/{trade.id}` on the Book button depending on whether the draft has an existing id.
   - Update popup's close/input action URLs to the new paths.

7. **Refactor DataGrid routes** — modify `src/main/java/com/bookie/components/DataGrid.java`.
   - Change `POST column-picker`, `POST sort/{columnName}`, `POST filter`, `POST viewport` to PUT.
   - Update all call sites that build these URLs (within `DataGrid.java` itself — `X.post(endpoint + "/sort/" + ...)` etc. become `X.put(...)`).

8. **Order the update channels** — modify all three screens' `setupRoutes` and the `BaseScreen.getUpdateURL()` / `connectUpdates` wiring.
   - Change each screen's update channel from `POST /{screen}/updates` to `GET /{screen}?updates`. Route via `RequestPredicates.GET("").and(RequestPredicates.queryParam("updates", __ -> true))` so `GET /{screen}` without the query param still matches the list/redirect handlers.
   - Update `BaseScreen.getUpdateURL()` return values: `SecuritiesScreen` → `"/security?updates"`, `TradesScreen` → `"/trades?updates"`, `PositionsScreen` → `"/positions?updates"`.
   - Update `Shell.java`'s `getUpdateRequestAttribute()` to emit `X.get(this.updateURL)` instead of `X.post(this.updateURL)` (keeping `withOpenWhenHidden` and the existing `retry: 'always'` options).
   - Rationale for putting ordering here: the updates fetch now sits at path `/{screen}`, which is a prefix of all resource ops under `/{screen}/...`. During initial connect and reconnect, resource ops block until SSE headers arrive, closing the race where a resource mutation's SSE patch could be lost on a not-yet-established channel. GET also lets `retry: 'always'` coalesce via idempotent replacement rather than piling up retry entries.

9. **Manual verification** — with a browser devtools Network tab open:
   - Stress test: rapidly change `issueSize` several times (type → backspace → type); confirm requests fire one at a time and the final server value matches the last input. Before this change, requests overlap and the final value was often wrong.
   - Edit/save/cancel race: click Edit → rapidly type in a field → click Save; Save must run after the last field update.
   - Add-row: spam-click "Add" on a schedule; confirm the number of entries added equals the number of clicks (POST add-row is non-idempotent and queues them all).
   - Cross-security navigation: switching cusip should not be blocked by in-flight edits on the previous cusip, since paths are cusip-scoped.
   - Trade lifecycle: open modify popup, edit fields, click Save → modifies the correct trade; open a new ticket, fill fields, click Book → creates a new trade. Delete from grid → trade disappears.
   - Grid viewport coalescing: scroll rapidly; confirm only one viewport PUT is in flight at a time and a final one is queued (idempotent replacement working).
   - Error cascade: kill the server mid-edit; confirm subsequent field updates fail immediately (cascaded) rather than timing out independently.

## Out of Scope

- Server-side use of `X-Idempotent` — the header is only forwarded when explicitly overriding, and handlers ignore it.
- Retry semantics at the ordering layer — DataStar's existing retry for SSE streams is unchanged. Each retry is a fresh fetch and creates a fresh queue entry.
- Replacing `fetch` for non-DataStar requests (there aren't any today).
- DataStar's `requestCancellation: 'auto'` — the plan assumes callers either leave it enabled (AbortErrors are handled gracefully) or disable it explicitly. No global change.
- Automated tests — manual verification per step is sufficient for this feature.

## Open Questions
None.
