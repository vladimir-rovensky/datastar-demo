# Feature: DataGrid Add Row

## Goal
Add an optional "add row" capability to the DataGrid component. When `.onAddRow(action)` is set, a "+" button appears in the action column header. Clicking it fires a PUT request to add a new blank entry server-side. The DataGrid also gains a configurable no-rows message shown when the row list is empty, and all Bond schedule collections are guaranteed non-null.

## Key Decisions
- **`onAddRow` takes `EscapedHtml`** (not a Function) — there is no row context when adding; the action is a static DataStar expression like `@put('/securities/resetSchedule')`.
- **Action column exists when either `addRowAction` or `getDeleteAction` is set** — they share the same first column; the header shows "+" only when `addRowAction` is set.
- **No-rows message renders inside `data-grid-body`** — `data-grid-body` is not a grid container, so a `<p class="centered-message">` naturally spans full width.
- **Empty-state grid is always rendered when editing** — callers (IncomeSection, RedemptionSection) drop the `isEmpty()` check when in edit mode so the "+" button is always reachable.
- **ID generation via `BondRepository.nextScheduleId()`** — a single `AtomicLong` counter initialized to `max(all existing schedule IDs as longs) + 1` after `generateData()`; returns `String.valueOf(counter.getAndIncrement())`. IDs are scoped per schedule list so uniqueness within a bond is all that matters, but a global counter is simpler and safe.
- **Collections initialized to empty `ArrayList`** — all four schedule fields in `Bond` default to `new ArrayList<>()` so null checks are never needed at the call site.

## Implementation Steps

1. **Bond.java — initialize schedule collections**
   - Change the four schedule field declarations to initialize to `new ArrayList<>()`:
     `private List<ResetEntry> resetSchedule = new ArrayList<>();` (and same for call/put/sinkingFund).
   - The null-guard branches in `clone()` can remain; they will simply never fire.

2. **BondRepository.java — add sequence counter**
   - Add field: `private final AtomicLong scheduleIdCounter;`
   - In the constructor, after `bonds = new ArrayList<>(generateData())`, compute `maxUsedId` by streaming all bonds and all four schedule lists, parsing each `getId()` as a long, finding the max (default -1), then set `scheduleIdCounter = new AtomicLong(maxUsedId + 1)`.
   - Add method: `public synchronized String nextScheduleId() { return String.valueOf(scheduleIdCounter.getAndIncrement()); }`

3. **DataGrid.java — add onAddRow / withAddRowTooltip / withNoRowsMessage**
   - Add fields:
     - `private EscapedHtml addRowAction;`
     - `private String addRowTooltip = "Add Row";`
     - `private String noRowsMessage = "Nothing here...";`
   - Add fluent methods: `onAddRow(EscapedHtml)`, `withAddRowTooltip(String)`, `withNoRowsMessage(String)`.
   - Update `getColumnStyleTemplate()`: action column exists when `addRowAction != null || getDeleteAction != null`.
   - Update `render()`:
     - Action header cell: render `<button>` with `data-on:click` and `data-tooltip` when `addRowAction != null`; otherwise render blank `<div class="data-grid-th">`.
     - Body: when `rows.isEmpty()`, render `<p class="centered-message">${noRowsMessage}</p>` instead of iterating rows.
   - Update `renderRow()` (no change needed — delete cell logic already checks `getDeleteAction != null`).

4. **IncomeSection.java — wire up add row for reset schedule**
   - In `getResetScheduleGrid`: remove the `isEmpty()` early-return branch entirely (the grid handles the empty state itself with `withNoRowsMessage`).
   - Add `.onAddRow(!disabled ? html("""@put('/securities/resetSchedule')""") : null)` and `.withNoRowsMessage("No Reset Schedule")` to the DataGrid builder.
   - The outer `null` check (`resetSchedule == null`) is no longer needed after Step 1; remove it.

5. **RedemptionSection.java — wire up add row for call/put/sinkingFund schedules**
   - Same treatment as Step 4 for all three grids:
     - Remove `isEmpty()` / null checks (guarded by `!disabled` on `onAddRow`).
     - Add `.onAddRow(!disabled ? html("""@put('/securities/callSchedule')""") : null)` (and similarly for put/sinkingFund).
     - Add `.withNoRowsMessage("No Call Schedule")` / `"No Put Schedule"` / `"No Sinking Fund Schedule"`.

6. **SecuritiesScreen.java — add PUT routes and handlers**
   - Register four new routes in `setupRoutes`:
     ```
     .PUT("resetSchedule", ...)
     .PUT("callSchedule", ...)
     .PUT("putSchedule", ...)
     .PUT("sinkingFundSchedule", ...)
     ```
   - Add handler `addResetEntry()`: call `bondRepository.nextScheduleId()`, create `new Bond.ResetEntry(newId, null, null)`, append to `editingBond.getResetSchedule()`, `triggerUpdate()`, return `ServerResponse.ok().build()`.
   - Add `addCallEntry()`, `addPutEntry()`, `addSinkingFundEntry()` following the same pattern with their respective entry types and list getters.

## Out of Scope
- Add-row support for grids other than the four Bond schedule grids.
- Inline validation of new blank rows.
- Sorting or reordering rows after add.

## Open Questions
- None — all design decisions resolved above.
