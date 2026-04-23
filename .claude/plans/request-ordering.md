# Feature: Client-side request ordering

## Goal
Guarantee that fetch requests issued by the client reach the server in the order they were issued, preventing racing POSTs (e.g. two rapid `issueSize` updates) from corrupting server state. The client enforces ordering by holding later requests until any earlier requests on the same URL resource — or any prefix of it — have settled. Idempotent requests coalesce so only the latest queued one fires. Failures cascade to anything waiting on the failed request, except for AbortErrors (which do not).

## Key Decisions

- **Resource identity = URL path**: Requests block each other if one path is a prefix of (or equal to) the other, comparing by `/`-separated segments. Example: `/securities/CSP1/issueDate` is blocked by `/securities/CSP1`, and `/securities/CSP1` is blocked by `/securities/CSP1/issueDate` (both directions — parent ↔ descendant always conflict). Siblings (`/securities/CSP1/issueDate` vs `/securities/CSP1/issueSize`) do not block.
- **Method is not part of the key**: GET/PUT/POST/DELETE on the same URL all participate in ordering. This is intentional — reads must observe prior writes, and different verbs on the same resource must serialize.
- **Query Params are not part of the key**
- **Idempotent = latest wins**: Marked via `X-Idempotent: true`. When an idempotent request arrives on a path that already has a queued (not yet running) request, the queued one is dropped (resolved with a synthetic 204) and replaced. Running requests are never preempted.
- **Default is idempotent**: `FetchBuilder` defaults `idempotent=true`. Only the handful of non-idempotent call sites (currently just DataGrid "add row") need to opt out via `.withIdempotent(false)`.
- **Error cascade**: If a blocker rejects, every request waiting on it rejects with the same error. Rationale: a failed predecessor usually means server state is suspect, so downstream requests on the same resource shouldn't proceed.
- **Aborts do not cascade**: AbortErrors are intentional and say nothing about server state, so they are filtered out of the cascade logic. Dependents proceed as if the aborted request settled normally.
- **Queue structure `Map<path, Entry[]>`**: Multiple entries per path are needed so non-idempotent requests on the same path actually queue (a single-slot map would let the newer one overwrite the running predecessor). Idempotent replacement targets the last entry in the list if it is queued.
- **Scope the wrapper to DataStar requests**: The monkey-patch activates only when `Datastar-Request` header is present. Other fetches (if any are ever added) pass through untouched. Matches the existing tabId guard on Shell:71.
- **Single combined monkey-patch file `fetch-monkeypatch.js`**: Folds the existing X-tabID wrapper from `Shell.java:67-76` and the new ordering logic into one `window.fetch` replacement. `tabId` is handed in via an inline `window.__tabID = '...'` script tag before the file is loaded.
- **Loaded before DataStar**: Script tag must come before `datastar1.0.0.RC8.js` so that `window.fetch` is wrapped before DataStar captures it in `Nn` via `qe = d || window.fetch`.
- **Routes become resource-oriented**: Current routes like `/securities/input/issueDate` and `/securities/save` are reshaped so the bond cusip sits in the URL. This is a prerequisite: without it, path-based ordering can't distinguish bonds and all updates collapse under `/securities/input/*` which doesn't match the intent.

## Route refactor

### SecuritiesScreen

| Current | New |
|---------|-----|
| `POST /securities/input/{field}` | `POST /securities/{cusip}/{field}` |
| `POST /securities/save` | `POST /securities/{cusip}` |
| `POST /securities/edit` | `POST /securities/{cusip}/edit` |
| `POST /securities/cancel` | `DELETE /securities/{cusip}/edit` |
| `POST /securities/resetSchedule` | `POST /securities/{cusip}/resetSchedule` |
| `PUT /securities/resetSchedule` | `PUT /securities/{cusip}/resetSchedule` |
| `DELETE /securities/resetSchedule/{id}` | `DELETE /securities/{cusip}/resetSchedule/{id}` |
| same for callSchedule / putSchedule / sinkingFundSchedule | `.../{cusip}/<schedule>[/{id}]` |
| `POST /securities/updates` | unchanged (screen-level SSE) |
| `GET /securities/{cusip}/{section}` | unchanged |

The cusip must be available on the client at render time so actions can embed it. Since `SecuritiesScreen` already knows the active cusip via `getCurrentCusip()`, render sites already have it.

### TradesScreen
No changes. `modify/{id}` and `delete/{id}` are popup-opener actions, and the prefix rule already serializes GET-confirm with POST-delete on `/trades/delete/{id}`. `DataGrid` routes under `/trades/grid/...` are independent.

### TradeTicketPopup
No changes. `/input` being in-flight disables the Book button in the UI, so the client-side ordering concern doesn't arise.

### PositionsScreen / DataGrid
No changes.

## Implementation Steps

1. **Create `fetch-monkeypatch.js`** — new file `src/main/resources/static/fetch-monkeypatch.js`.
   - Wraps `window.fetch`. Only intercepts requests with `Datastar-Request` header; pass-through otherwise.
   - Adds `X-tabID: window.__tabID` to intercepted requests (ports the existing Shell:67 logic).
   - Maintains `const queue = new Map()` where values are `Entry[]`. `Entry = { promise, resolve, reject, state }`.
   - `blocks(a, b)` splits both paths on `/`, filters empty segments, returns true if one segment list is a prefix of the other.
   - `isIdempotent(init)` checks `init.headers` for `X-Idempotent` (both plain-object and Headers-instance forms). FetchBuilder emits fixed casing, so no case-insensitive scan needed.
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
   - New field `private boolean idempotent = true;`.
   - New method `public FetchBuilder withIdempotent(boolean value)`.
   - In `buildOptions()`: when `idempotent` is true, append `headers: {'X-Idempotent': 'true'}` to the DataStar options object (merged with existing headers option if any — currently there isn't one, so this is the only source of `headers:`).

4. **Opt out of idempotent at DataGrid add-row call sites** — `.withIdempotent(false)` on every `X.put(...)` that feeds `onAddRow(...)`:
   - `IncomeSection.java:112`
   - Any other `onAddRow(!disabled ? X.put(...).render() : null)` call sites (callSchedule / putSchedule / sinkingFundSchedule counterparts in `IncomeSection` and `RedemptionSection` — verify by grep on `onAddRow`).

5. **Refactor SecuritiesScreen routes** — modify `src/main/java/com/bookie/screens/securities/SecuritiesScreen.java`.
   - Update `setupRoutes` per the Route refactor table: move the schedule routes and per-field input route under `/{cusip}/...`, rename save to `POST /{cusip}`, edit/cancel to `POST /{cusip}/edit` and `DELETE /{cusip}/edit`.
   - Update the handler methods to read `{cusip}` from the path and validate it matches `currentBond.getCusip()` (or `editingBond`). Non-matching cusip → return 409/410 so stale clients don't mutate the wrong bond.
   - Update all client call sites that build these URLs: `renderEditActions` (edit/save/cancel), `GeneralSection` / `IncomeSection` / `RedemptionSection` `data-on:change` action, and schedule add/delete actions. All of these already have the cusip in scope via `getActiveBond().getCusip()` or similar — thread it through to the URL template.
   - Cusip must round-trip through the URL in every call; confirm the rendered action strings interpolate the cusip into a static literal (no reliance on the signal store).

6. **Manual verification** — with a browser devtools Network tab open:
   - Stress test: rapidly change `issueSize` several times (type → backspace → type); confirm in network tab that requests fire one at a time and the final server value matches the last input. Before this change, requests overlap and the final value was often wrong.
   - Edit/save/cancel race: click Edit → rapidly type in a field → click Save; Save must run after the last field update.
   - Add-row: spam-click "Add" on a schedule; confirm the number of entries added equals the number of clicks (non-idempotent queues all of them rather than coalescing).
   - Cross-screen: switching cusip (navigating to another security) should not be blocked by in-flight edits on the previous cusip, since paths are cusip-scoped.
   - Error cascade: kill the server mid-edit; confirm subsequent field updates fail immediately (cascaded) rather than timing out independently.

## Out of Scope

- Server-side use of `X-Idempotent` — the header is forwarded but ignored by handlers.
- Retry semantics at the ordering layer — DataStar's existing retry behavior for SSE streams is unchanged. Each retry is a fresh fetch and creates a fresh queue entry.
- Replacing `fetch` for non-DataStar requests (there aren't any today).
- DataStar's `requestCancellation: 'auto'` — the plan assumes callers either leave it enabled (AbortErrors are handled gracefully) or disable it explicitly. No global change.
- Tests — manual verification per step is sufficient for this feature; no automated test harness added.

## Open Questions
None.
