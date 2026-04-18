# Feature: DataGrid Column Sorting

## Goal
Let users sort the rows of a `DataGrid` by clicking a column header. The first click
sorts ascending, the second descending, the third clears the sort. The active sort
column shows a small unicode triangle (`▲`/`▼`) aligned to the right edge of its
header cell. Sorting is server-driven: a click POSTs to a new `/sort/{columnName}`
route, the grid updates its sort state, and triggers a re-render through the existing
update channel.

## Key Decisions

- **Introduce `withEndpoint(String)` on `DataGrid`**: a single base path used to build
  all of the grid's action URLs (column picker, sort, and anything we add later).
  `DataGridColumnPicker` currently takes its base path as an argument to
  `.withColumnPicker(String)` — that method becomes parameterless and only sets a
  boolean `showColumnPicker`. URLs for the column picker now derive from the endpoint.
  Alternative considered: leave `withColumnPicker` taking a path and add a separate
  `sortBasePath` — rejected because we'd end up passing the same path twice and new
  features would keep multiplying the problem.

- **Throw if a feature needs the endpoint but it isn't set**: calling
  `.withColumnPicker()` or clicking a sortable header when `endpoint` is null should
  throw a clear `RuntimeException` (at render or route-setup time). Keeps the contract
  explicit.

- **Sort direction as an enum**: nested enum `DataGrid.SortDirection { Ascending, Descending }`.
  Null `sortColumnName` means "no sort applied".

- **State machine on the server**: click on column X maps to:
    - `sortColumnName == null` or `sortColumnName != X` → set to X, Ascending
    - `sortColumnName == X` and direction Ascending → keep X, Descending
    - `sortColumnName == X` and direction Descending → clear (both to null)

- **Sort applied inside `render()` via a stream**: the incoming `this.rows` is never
  mutated. Use `this.rows.stream().sorted(comparator).toList()` and render that.
  Comparator is built from the column's `getValue`, casting to `Comparable`. If the
  first non-null value is not `Comparable`, silently skip the sort (return rows
  unchanged). This matches the user's instruction: "if it isn't [comparable] we can
  ignore the sort attempt."

- **Sort persists across row updates**: because sort state lives on the grid and is
  applied inside `render()`, any event-driven `triggerUpdate()` (new trade booked,
  trade modified, etc.) automatically re-applies the current sort. Caching of the
  sorted list is **out of scope** for this plan — we re-sort on every render for now.

- **Hiding a sorted column via the column picker clears the sort**: in
  `applyColumnPicker`, after applying the new visibility, if the currently sorted
  column is no longer visible, clear `sortColumnName` and `sortDirection`.

- **Header cell click wiring**: when `endpoint` is set, every visible column header
  renders with `data-on:click="@post('${endpoint}/sort/${columnName}')"` and gets
  `cursor: pointer`. The column name in the URL is `DataGridColumn.getName()` (header
  with spaces replaced by underscores — already used elsewhere). Non-comparable columns
  still receive the click handler; the server simply does nothing on that request.
  Accepting this minor UX wart is consistent with the user's "just ignore" rule.

- **Indicator glyph**: inline `<span class="data-grid-sort-indicator">▲</span>` (or
  `▼`) rendered inside the header cell after the header text, for the active sort
  column only. CSS pushes it to the right edge via `margin-left: auto` in a flex
  header cell.

- **Routing**: add `POST sort/{columnName}` to `DataGrid.setupRoutes`, alongside the
  existing column-picker routes. `TradesScreen` already nests
  `DataGrid.setupRoutes` under `/grid`, so no caller-side changes are needed.

## Implementation Steps

### Step 1 — Refactor: introduce `withEndpoint`, make `withColumnPicker` parameterless

Prep step. No new user-visible feature — verifies the existing column picker still
works through the new API.

- `DataGrid.java`:
    - Replace the `columnPickerBasePath` field with:
        - `private String endpoint;`
        - `private boolean showColumnPicker = false;`
    - Add `public DataGrid<TRow> withEndpoint(String endpoint)`.
    - Change `withColumnPicker(String basePath)` → `withColumnPicker()` (no args),
      sets `showColumnPicker = true`.
    - In `openColumnPicker`: build `pickerPath` from `endpoint + "/column-picker"`.
      If `endpoint == null`, throw.
    - In `getActionHeaderCell`: when rendering the column-picker button, use
      `endpoint` for the URL; guard: if `showColumnPicker` but `endpoint` is null,
      throw.
    - Update `hasActionColumn()` to check `showColumnPicker` instead of
      `columnPickerBasePath != null`.
- `TradesScreen.java`:
    - Change `.withColumnPicker("/trades/grid")` to
      `.withEndpoint("/trades/grid").withColumnPicker()`.
- **Manual verification**: run the app, open the Trades screen, open the column
  picker, toggle a column, hit OK → the column visibility change should work as
  before.

### Step 2 — Sort state, route, and click-through cycle (no visual indicator yet)

Smallest end-to-end slice that is observable: clicking a header actually changes row
order. Cycle works. No triangle yet — that's step 3.

- `DataGrid.java`:
    - Add nested enum:
      ```java
      public enum SortDirection { Ascending, Descending }
      ```
    - Add fields:
      ```java
      private String sortColumnName; // null means no sort
      private SortDirection sortDirection;
      ```
    - Add method `public ServerResponse applySort(ServerRequest request)`:
        - Read `columnName` path variable.
        - Find the matching column by `DataGridColumn.getName()`. If not found, return
          `ServerResponse.ok().build()` (silent no-op).
        - Update `sortColumnName` / `sortDirection` using the state machine in "Key
          Decisions".
        - Call `reRender()`.
        - Return `ServerResponse.ok().build()`.
    - In `setupRoutes`: register
      `.POST("sort/{columnName}", request -> getGrid.apply(request).applySort(request))`.
    - In `render()`: when `sortColumnName != null`, find the matching column; if its
      first non-null value across rows is a `Comparable`, build a comparator from
      `column.getValue`, reverse it for `Descending`, and feed rows through
      `this.rows.stream().sorted(comparator).toList()`. If not `Comparable`, render
      the unsorted rows. If the column isn't found (e.g. removed), clear the sort
      state and render unsorted.
    - In the header-cell rendering: if `endpoint` is set, wrap every visible column
      header with
      `data-on:click="@post('${endpoint}/sort/${columnName}')"`.
      If `endpoint` is null, render the header unchanged (no click).
- **Manual verification**:
    - Click the "Trade Date" header on the Trades screen — rows sort ascending.
    - Click again — rows sort descending.
    - Click again — original order restored.
    - Click a different column — rows sort by that column ascending.
    - Book a new trade while sorted — new trade slots into the correct sorted position.
    - Click a non-comparable column header (e.g. CUSIP) — grid re-renders with rows
      unchanged. Acceptable.

### Step 3 — Sort indicator glyph and header styling

Purely presentational. Shows which column is sorted and in which direction.

- `DataGrid.java`:
    - When rendering header cells, for the column whose name matches
      `sortColumnName`, append an inline span after the header text:
      `<span class="data-grid-sort-indicator">▲</span>` for `Ascending`,
      `<span class="data-grid-sort-indicator">▼</span>` for `Descending`.
    - Add a `sortable` CSS class to header cells that have a click handler (i.e.
      when `endpoint` is set), so CSS can style the cursor.
- `global-styles.css`:
    - Update `.data-grid-th` to use flex layout: `display: flex; align-items: center;`
      so the indicator span can sit on the right edge.
    - Add `.data-grid-th.sortable { cursor: pointer; }`.
    - Add `.data-grid-sort-indicator { margin-left: auto; padding-left: var(--sp-xs); }`.
- **Manual verification**:
    - Cursor turns into a pointer on hover over column headers.
    - Click "Trade Date" → `▲` appears on the right edge of that header.
    - Click again → `▼`.
    - Click again → no indicator.
    - Indicator follows the sort as you click different columns.

### Step 4 — Clear sort when the sorted column is hidden

Edge case for consistency with the column picker.

- `DataGrid.java`:
    - In `applyColumnPicker`, after updating visibility on the columns, if
      `sortColumnName != null` and the matching column's `visible` is now `false`,
      clear `sortColumnName` and `sortDirection`. `reRender()` already runs after.
- **Manual verification**:
    - Sort by "Trade Date" (triangle visible).
    - Open column picker, uncheck "Trade Date", click OK.
    - The grid re-renders with "Trade Date" hidden and no latent sort (rows are in
      original order).
    - Re-show "Trade Date" via picker — the column comes back unsorted.

## Out of Scope
- Caching the sorted row list to avoid re-sorting on every render.
- Per-column opt-out of sorting (`column.withSortable(false)`).
- Multi-column / tie-breaker sort.
- Persisting sort state across page reloads.
- Visual styling of non-sortable column headers (they still get `cursor: pointer`).

## Open Questions
None — all decisions confirmed by the user during the brainstorming session.
