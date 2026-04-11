# Feature: Securities Screen

## Goal

Add a Securities screen for viewing and editing bond reference data. It sits alongside Trades and
Positions in the nav. The user loads a bond by CUSIP via a lookup input; the bond is displayed in
three subsections (General, Income, Redemption) with a secondary toolbar for navigation. The screen
has read mode (default) and edit mode. Edit mode allows changing all scalar fields except CUSIP;
Save persists changes to the repository, Cancel discards them.

## Key Decisions

- **Full-page navigation**: Subsection tabs and CUSIP lookup both cause a full page load. The URL
  always reflects the current bond and section (`/securities/{cusip}/{section}`). Consistent with
  how Trades and Positions work (plain MPA, no boost for now).

- **CUSIP lookup**: Client-side navigation only — `data-on:keydown.enter` and a Load button both
  execute `window.location.href = '/securities/' + $cusipLookup.trim() + '/{section}?tabID={tabId}'`.
  No server round-trip needed. Fires only when input is non-empty.

- **CUSIP is non-editable**: Even in edit mode, CUSIP is displayed as plain text. Changing it would
  invalidate all trades referencing it.

- **Schedules (list fields)**: Populated with sample data in BondRepository. Rendered as read-only
  tables in the Income and Redemption sections. Editing list-type schedule entries is out of scope
  for this iteration.

- **Edit/Save/Cancel buttons**: Rendered via `getToolbarContent()` override so they appear in the
  main toolbar bar (left side), matching the TradeTicket button placement on other screens.

- **Secondary toolbar**: Rendered at the top of `getContent()` — contains the CUSIP input/Load
  button and the three section links (General | Income | Redemption).

- **Unified URL scheme**: A single route pattern `/{cusip}/{section}` handles all states. The
  sentinel value `nocusip` is used when no bond is loaded (e.g. `/securities/nocusip/general`).
  `GET ""` redirects to `/securities/nocusip/general`. This eliminates the ambiguity of a
  one-segment `/{section}` route and makes section links always the same shape.

- **Empty state**: When no CUSIP is loaded (`cusip == "nocusip"`), the content area shows a
  centred message: "Please load a CUSIP in the top-left."

## Implementation Steps

### 1. BondRepository — make mutable and add saveBond

File: `src/main/java/com/bookie/domain/entity/BondRepository.java`

- Remove `Collections.unmodifiableList(...)` wrapper; use a plain `new ArrayList<>(generateData())`.
- Add method `saveBond(Bond bond)`: replaces the existing entry whose CUSIP matches `bond.getCusip()`.
  Use `bonds.replaceAll(existing -> existing.getCusip().equals(bond.getCusip()) ? bond : existing)`.

Verify: app starts, existing screens work normally.

### 2. BondRepository — populate sample schedules

File: `src/main/java/com/bookie/domain/entity/BondRepository.java`

- In `buildCorporate`, add call schedule entries:
  - Maturity year > 2031: two `CallEntry` records — (maturityDate minus 5 years, 101.00) and
    (maturityDate minus 2 years, 100.50).
  - Maturity year 2028–2031: one `CallEntry` at (maturityDate minus 1 year, 100.00).
- In `buildCorporate`, add a put schedule for bonds where `cusip.hashCode()` is even:
  one `PutEntry` at (issueDate plus 3 years, 100.00).
- In `buildCorporate`, add a sinking fund for bonds with sector "Industrials": two
  `SinkingFundEntry` records — (maturityDate minus 3 years, 25% of issueSize) and
  (maturityDate minus 1 year, 25% of issueSize).

Verify: app starts without errors.

### 3. Shell — add Securities to nav and prerender

File: `src/main/java/com/bookie/screens/Shell.java`

- In `buildNav()`: add a Securities `<a>` tag rendered as plain HTML (not the `Link` component —
  that only handles single-segment paths). Target: `/securities/nocusip/general?tabID={tabId}`.
  Add `aria-current="page"` when `title.equals("Securities")`.
- In the `<script type="speculationrules">` block, add `/securities/nocusipgeneral?tabID=${tabId}`
  to the prerender URLs array.

Verify: nav shows three links; Securities link navigates (will 404 until step 5).

### 4. Router — register SecuritiesScreen routes

File: `src/main/java/com/bookie/Router.java`

- Add `.nest(path(SecuritiesScreen.RoutePrefix), () -> SecuritiesScreen.setupRoutes(sessionRegistry))`
  alongside the existing Trades and Positions nests.

Note: `SecuritiesScreen` does not exist yet — this step must be done together with step 5, or the
import will fail to compile. Merge steps 4 and 5 into one commit.

### 5. SecuritiesScreen — skeleton: routing, state, initial render, empty state

File: `src/main/java/com/bookie/screens/SecuritiesScreen.java` (new)

**Static constants:**
```
public static final String RoutePrefix = "/securities";
public static final String NO_CUSIP    = "nocusip";
```

**State fields:**
```
private Bond currentBond;      // null when no CUSIP loaded or CUSIP not found
private Bond editingBond;      // null in read mode; a deep copy in edit mode
private String currentCusip;   // the cusip segment from the URL (may be NO_CUSIP)
private String currentSection; // "general", "income", or "redemption"
```

**Constructor**: receives `BondRepository` via injection. Calls `super("Securities")`.
Sets `currentSection = "general"`. No event subscriptions.

**setupRoutes(SessionRegistry sessionRegistry):**
```
GET  ""                   → redirect to /securities/nocusip/general
GET  "/{cusip}/{section}" → getOrCreateSession → screen.initialRender(request)
POST "updates"            → connectUpdates(sessionRegistry, SecuritiesScreen.class, request)
POST "edit"               → sessionRegistry.getScreen(request, SecuritiesScreen.class).startEdit()
POST "save"               → sessionRegistry.getScreen(request, SecuritiesScreen.class).saveEdit(request)
POST "cancel"             → sessionRegistry.getScreen(request, SecuritiesScreen.class).cancelEdit()
```

**initialRender(ServerRequest request):**
- `synchronized`
- Read `cusip` and `section` from path variables.
- Set `currentSection = section` (default "general" if blank).
- If `cusip.equals(NO_CUSIP)`: set `currentBond = null`, `currentCusip = NO_CUSIP`.
- Else: set `currentCusip = cusip`; look up via `bondRepository.findBondByCusip(cusip)`;
  set `currentBond` to result (null if not found).
- Call `handleInitialRender(request, this::render)`.

**getUpdateURL():** returns `RoutePrefix + "/updates"`

**getToolbarContent():** return `EscapedHtml.blank()` for now (wired up in step 7).

**getContent():** return a `<div id="securities-screen">` with the empty state message:
"Please load a CUSIP in the top-left." (secondary toolbar and section content added in later steps).

Verify: navigating to `/securities` redirects to `/securities/nocusip/general`; the Securities
screen renders with the nav link active and the empty state message visible.

### 6. SecuritiesScreen — secondary toolbar (CUSIP input + section links)

File: `src/main/java/com/bookie/screens/SecuritiesScreen.java`

Add `renderSecondaryToolbar()` and call it from `getContent()` above the empty state / section content.

**CUSIP input:**
- Text input bound to DataStar signal `cusipLookup`, pre-filled server-side with `currentCusip`
  (empty string when `currentCusip` is `NO_CUSIP`).
- `data-on:keydown.enter` fires client-side navigation:
  `if($cusipLookup.trim()) window.location.href='/securities/'+$cusipLookup.trim()+'/${section}?tabID=${tabId}'`
  where `section` and `tabId` are interpolated server-side at render time.
- A "Load" button with the same action.

**Section links:** three `<a>` tags always using the two-segment form. Cusip segment is
`currentCusip` (which may be `NO_CUSIP`):
- General → `/securities/{cusip}/general?tabID={tabId}`
- Income  → `/securities/{cusip}/income?tabID={tabId}`
- Redemption → `/securities/{cusip}/redemption?tabID={tabId}`

Active link (matching `currentSection`) gets `aria-current="page"`.

Verify: typing a known CUSIP and pressing Enter (or Load) navigates to that bond's URL. Section
links switch between tabs and update the active state. Unknown CUSIP shows empty state.

### 7. SecuritiesScreen — edit mode (toolbar buttons + state transitions)

File: `src/main/java/com/bookie/screens/SecuritiesScreen.java`

**getToolbarContent():**
- If `currentBond == null`: return `EscapedHtml.blank()`.
- If `editingBond == null` (read mode):
  `<button data-on:click="@post('/securities/edit')">Edit</button>`
- If `editingBond != null` (edit mode):
  `<button class="btn-primary" data-on:click="@post('/securities/save', {contentType: 'form'})">Save</button>`
  `<button data-on:click="@post('/securities/cancel')">Cancel</button>`

**startEdit():** `synchronized` — deep-copy all fields from `currentBond` into a new `Bond`
instance assigned to `editingBond`. Call `triggerUpdate()`.

**cancelEdit():** `synchronized` — set `editingBond = null`. Call `triggerUpdate()`.

**saveEdit(ServerRequest request):** `synchronized` — read form params and set all scalar fields
on `editingBond` except `cusip`. Call `bondRepository.saveBond(editingBond)`. Set
`currentBond = editingBond`, `editingBond = null`. Call `triggerUpdate()`.

Note: `saveEdit` will only write to fields that have form inputs — wired up fully in steps 8–10.
At this stage, Save just round-trips with no field changes, which is fine for verification.

Verify: Edit button appears when a bond is loaded. Clicking Edit shows Save/Cancel. Cancel reverts
to read mode. Save also reverts (no visible field changes yet).

### 8. SecuritiesScreen — General section form

File: `src/main/java/com/bookie/screens/SecuritiesScreen.java`

Add `renderGeneral()`. Wire `renderSection()` dispatcher and call it from `getContent()`.
Bond source for all fields: `editingBond != null ? editingBond : currentBond`.
All inputs get `withDisabled(editingBond == null)`.

Fields in a two-column CSS grid (use `@scope` block for scoped styles, following the pattern in
`TradesScreen`):
- CUSIP: plain text `<span>`, never an input
- ISIN, Ticker, Issuer Name, Description: `TextInput`
- Bond Type: `SelectInput` (options from `BondType.values()`)
- Sector, Currency, Country, Seniority Level: `TextInput`
- Issue Date, Dated Date, Maturity Date, First Coupon Date: `DateInput`
- Issue Size, Face Value, Issue Price: `NumberInput`
- Moody's Rating, S&P Rating, Fitch Rating: `TextInput`
- Secured: `SelectInput` with options `["true", "false"]`

In `saveEdit`, read and apply these form params to `editingBond`.

Verify: General tab shows all fields. In read mode all are disabled. Edit mode enables them.
Save persists changes (reload the page to confirm the updated values come back from the repo).

### 9. SecuritiesScreen — Income section form

File: `src/main/java/com/bookie/screens/SecuritiesScreen.java`

Add `renderIncome()` to the `renderSection()` dispatcher.

Editable scalar fields (same disabled/source logic as General):
- Coupon Type: `SelectInput` (options from `CouponType.values()`)
- Coupon, Spread: `NumberInput`
- Coupon Frequency: `NumberInput`
- Day Count: `SelectInput` (options from `DayCountConvention.values()`)
- Floating Index: `TextInput`

Read-only reset schedule table below the form (always disabled, regardless of edit mode):
columns Reset Date and New Rate. If `resetSchedule` is null or empty, show "No reset schedule."

In `saveEdit`, read and apply the Income scalar params to `editingBond`.

Verify: Income tab renders editable coupon fields and the (empty) reset schedule table.

### 10. SecuritiesScreen — Redemption section

File: `src/main/java/com/bookie/screens/SecuritiesScreen.java`

Add `renderRedemption()` to the `renderSection()` dispatcher. All content is read-only.

- Call schedule table: columns Call Date and Call Price. If null/empty, "No call schedule."
- Put schedule table: columns Put Date and Put Price. If null/empty, "No put schedule."
- Sinking fund table: columns Sink Date and Amount. If null/empty, "No sinking fund schedule."

Verify: Redemption tab shows populated schedule tables for bonds that have them (e.g. Industrials
sector bonds have sinking fund entries from step 2).

### 11. TradesScreen and PositionsScreen — CUSIP as hyperlink

Files:
- `src/main/java/com/bookie/screens/TradesScreen.java`
- `src/main/java/com/bookie/screens/PositionsScreen.java`

In each screen's `DataGrid` CUSIP column, replace the method reference with a lambda returning:
```
html("""
    <a href="/securities/${cusip}/general?tabID=${tabId}">${cusip}</a>
    """, "cusip", <row>.getCusip(), "tabId", getTabID().localID())
```

`EscapedHtml` implements `Renderable`, so the templating engine passes it through unescaped — no
changes needed to `DataGrid`.

Verify: CUSIP values in both grids are clickable links that navigate to the correct bond on the
Securities screen.

## Out of Scope

- Editing list-type fields (reset schedule, call schedule, put schedule, sinking fund schedule).
- Adding or removing bonds (no create/delete, only load and update).
- Validation of field values on save (beyond what the type system enforces).
- Navigation boosting / client-side routing for section tabs.

## Open Questions

- CSS for the two-column form grid in the General section — implementer should follow the
  `@scope` pattern already used in TradesScreen for scoped styles.
