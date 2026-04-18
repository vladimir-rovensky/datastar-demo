# Feature: Column Picker for DataGrid

## Goal
Add a column visibility picker to the TradesScreen and PositionsScreen grids. Users can show/hide columns via a popup triggered by a hover button on the grid header. Both screens gain 10 additional Bond-derived columns that are hidden by default.

## Key Decisions

- **Visibility state on DataGridColumn**: add `boolean visible = true` with a `withVisible(boolean)` fluent setter. The `DataGrid` filters to visible-only columns when building headers, rows, and the CSS grid template.
- **Grid stored as a field on each Screen**: columns live inside the `DataGrid` instance. The Screen stores the `DataGrid` as a field (created once in the constructor). `getContent()` calls `this.grid.withRows(...).render()` each time. The column picker routes mutate column visibility directly on the stored grid instance.
- **`DataGrid` exposes its columns**: add a `getColumns()` method returning the full column list (including hidden ones) so picker routes can read and update visibility.
- **`CommonColumns` helper class**: a new class `com.bookie.components.CommonColumns` with static factory methods for the 10 shared Bond-derived columns. Both TradesScreen and PositionsScreen import from it. Takes a `Function<TRow, Optional<Bond>>` to resolve the bond for a given row.
- **`formatRatings` extracted to `Format`**: `Format.ratings(Bond)` returns e.g. `"Aaa / AA+ / AA"`, using `"—"` for any null/blank rating. Shared by both screens via `CommonColumns`.
- **Array signal for MultiselectInput**: initialize `data-signals:name="[...]"` as a JSON array; `data-bind:name` on `<select multiple>` gives DataStar an array of selected option values automatically.
- **CSS-only hover button**: the column picker button is `position: absolute` inside a `position: relative` wrapper on the header; shown via `:hover` CSS. No DataStar signals needed.
- **Route naming**: `GET /trades/grid/column-picker` opens the popup; `POST /trades/grid/column-picker` applies changes. Same pattern for `/positions/grid/column-picker`.
- **Reading the signal on POST**: `request.body(new ParameterizedTypeReference<Map<String, List<String>>>() {})` to get `{"visibleColumns": [...]}`.

## Implementation Steps

### Step 1 — Add `visible` to `DataGridColumn`; filter in `DataGrid`; expose `getColumns()`
- In `DataGridColumn.java`: add `boolean visible = true`; add `withVisible(boolean visible)` fluent setter.
- In `DataGrid.java`:
  - Add `public List<DataGridColumn<TRow>> getColumns()` returning `this.columns`.
  - Introduce a private `getVisibleColumns()` returning `columns.stream().filter(c -> c.visible).toList()`.
  - Replace every use of `columns` in `render()`, `getColumnStyleTemplate()`, and `renderRow()` with `getVisibleColumns()`.
  - `getColumnStyleTemplate()` must use `getVisibleColumns().size()`.
- Verify: temporarily set a column to `visible=false` in TradesScreen and confirm it disappears.

### Step 2 — Create `MultiselectInput` component
- New file `src/main/java/com/bookie/components/MultiselectInput.java`.
- Model on `SelectInput` (extend `BaseInput`).
- Fields: `List<String> options`, `List<String> value` (the initially selected values).
- Factory method: `static MultiselectInput multiselectInput(String name, List<String> options, List<String> value)`.
- `render()`:
  - Initialize the array signal: `data-signals:${kebabName}="${jsonArray}"` where `jsonArray` is the `value` list serialized as a JSON array (use `Util.toJson`).
  - Render `<select multiple data-bind:${kebabName}>` containing one `<option value="${v}">${v}</option>` per option (no `selected` attribute — DataStar handles pre-selection via the signal).
- Signal name is kebab-cased from `name` (same as `BaseInput` does).
- Verify: render a `MultiselectInput` standalone and inspect the HTML for correct signal and binding attributes.

### Step 3 — Add column picker button to `DataGrid` header; add CSS
- In `DataGrid.java`:
  - Add field `EscapedHtml columnPickerAction` (null by default).
  - Add fluent method `withColumnPickerAction(EscapedHtml columnPickerAction)`.
  - In `render()`, wrap the existing `data-grid-header` div in a `data-grid-header-wrapper` div. When `columnPickerAction != null`, render a picker button inside the wrapper (after the header div):
    ```
    <div class="data-grid-header-wrapper">
        <div class="data-grid-header">…</div>
        <button class="column-picker-btn btn-no-bg" data-on:click="${action}" data-tooltip="Pick columns">≡</button>
    </div>
    ```
  - When `columnPickerAction` is null, still wrap in `data-grid-header-wrapper` but omit the button.
- In `global-styles.css`, add:
  ```css
  .data-grid-header-wrapper {
      position: relative;
  }
  .column-picker-btn {
      display: none;
      position: absolute;
      right: 4px;
      top: 50%;
      transform: translateY(-50%);
  }
  .data-grid-header-wrapper:hover .column-picker-btn {
      display: flex;
  }
  ```
- Verify: wire up a dummy `withColumnPickerAction` on the TradesScreen grid and confirm the button appears on hover.

### Step 4 — Add `CommonColumns` and `Format.ratings`
- Add `static String ratings(Bond bond)` to `Format.java`. Returns e.g. `"Aaa / AA+ / AA"`, substituting `"—"` for any null or blank individual rating.
- New file `src/main/java/com/bookie/components/CommonColumns.java`.
- Contains one static method per Bond column, each accepting a `Function<TRow, Optional<Bond>> getBond` parameter and returning a `DataGridColumn<TRow>` with `.withVisible(false)`:
  1. `maturityDate(getBond)` → `"Maturity Date"`
  2. `issueDate(getBond)` → `"Issue Date"`
  3. `coupon(getBond)` → `"Coupon"`
  4. `couponType(getBond)` → `"Coupon Type"`
  5. `bondType(getBond)` → `"Bond Type"`
  6. `currency(getBond)` → `"Currency"`
  7. `sector(getBond)` → `"Sector"`
  8. `country(getBond)` → `"Country"`
  9. `ratings(getBond)` → `"Moody's/S&P/Fitch"` — uses `Format.ratings(bond)`, falls back to `"—"` when bond is absent
- Verify: the class compiles and its methods can be called with a lambda.

### Step 5 — Wire `TradesScreen` to use a stored `DataGrid`; add picker routes
- In `TradesScreen.java`:
  - Add field `DataGrid<Trade> tradeGrid`, initialized in the constructor with all columns: the existing 10 columns plus the 9 from `CommonColumns` (called with `t -> getBond(t.getCusip())`). Also call `.withColumnPickerAction(html("@get('/trades/grid/column-picker')"))` and all the other existing grid config (row ID, double-click, delete, tooltip, striped rows) — everything except `.withRows(...)`.
  - In `getContent()`, replace the inline `DataGrid.withColumns(...)` call with `this.tradeGrid.withRows(this.trades.reversed()).render()`.
  - In `setupRoutes()`, add:
    - `GET("grid/column-picker", ...)` → `openColumnPicker(request)`
    - `POST("grid/column-picker", ...)` → `applyColumnPicker(request)`
  - `openColumnPicker`: reads `this.tradeGrid.getColumns()` to compute `allHeaders` and `visibleHeaders`. Opens a `Popup` with title `"Columns"`, content = `MultiselectInput.multiselectInput("visibleColumns", allHeaders, visibleHeaders)`, actions = OK button (`@post('/trades/grid/column-picker')`) and Cancel (`@post('/tradeTicket/cancel')`).
  - `applyColumnPicker`: reads body as `Map<String, List<String>>`, extracts `visibleColumns`, updates each column's `visible` flag via `column.visible = visibleColumns.contains(column.header)`. Calls `triggerUpdate()`. Returns `Popup.close()`.
- Verify: Trades screen column picker opens, toggles work, OK re-renders grid with correct columns.

### Step 6 — Wire `PositionsScreen` to use a stored `DataGrid`; add picker routes
- Mirror Step 5 for `PositionsScreen`:
  - Add field `DataGrid<Position> positionsGrid` with existing 6 columns plus `CommonColumns` columns (called with `p -> getBond(p.getCusip())`), plus `.withColumnPickerAction(html("@get('/positions/grid/column-picker')"))`.
  - In `getContent()`, replace inline grid construction with `this.positionsGrid.withRows(this.positions).render()`.
  - Add `GET("grid/column-picker", ...)` and `POST("grid/column-picker", ...)` to `setupRoutes()` with the same open/apply pattern.
- Verify: same manual test on Positions screen.

## Out of Scope
- Persisting column visibility across browser sessions or server restarts.
- Per-user visibility preferences.
- Column reordering or resizing.

## Open Questions
- The `≡` character renders differently across fonts/OSes — if it looks wrong on Windows, consider a small SVG icon instead.
