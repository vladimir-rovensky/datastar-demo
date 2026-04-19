# Feature: DataGrid filter row

## Goal
Add optional filter capability to `DataGrid`. When enabled via a new `.filterable()` prop, the grid renders a second header row containing a `TextInput` per visible data column. The action column (if present) renders an empty cell — it is not filterable. Typing in a filter input and blurring (or pressing Enter) posts to a new `/filter` endpoint on the grid, which updates server-side filter state and re-renders the grid. Filter logic lives in a separate `GridFilter` class and supports string (comma-separated contains, OR), number (ordinal operators and `N-M` range), date (same operators, `MM/dd/yyyy` format for both `LocalDate` and `java.util.Date`), boolean (`Y`/`N`), and fallback-to-string for anything else. Null cell values never match. Filter evaluates on unformatted values (ignoring both `column.format` and `column.renderer`). Filtering runs before sorting.

## Key Decisions

- **Grid ID format**: switch from UUID to a simple sequential counter (`grid1`, `grid2`, …) sourced from a `static AtomicInteger` on `DataGrid`. Reason: avoids hyphens and length issues in DataStar signal paths. Alternative considered: keep UUID with custom sanitization — rejected as overkill.

- **Signal namespacing**: each filter input binds to `${gridId}.filter.${columnName}` (e.g. `grid1.filter.last_activity`). Reason: supports multiple grids on a page and groups filter signals under the grid. Alternative considered: per-column endpoint with flat signal name — rejected; one endpoint is simpler.

- **Endpoint**: single `POST {endpoint}/filter`. DataStar posts all current signals on change; handler reads the nested `grid<N>.filter.*` map and updates all column filters in one shot.

- **Event trigger**: `data-on:change` (fires on blur / Enter). Reason: minimal chatter, natural UX. Alternative considered: debounced `input` — rejected for demo simplicity.

- **Filter state storage**: `Map<String, String>` (columnName → filter text) field on `DataGrid`, mirroring how `sortColumnName`/`sortDirection` live on the grid.

- **Type detection**: per visible column, find the first row with a non-null raw value and use its runtime type to pick the filter branch. Type is detected at filter-evaluation time, not cached across renders. If every row is null for that column, the filter yields no matches (the filter is effectively non-matching for that column).

- **Filter logic shape**: one method per type in `GridFilter`, dispatching on `instanceof`. Ordinal operators share one generic helper `matchesOrdinal(Comparable<T> value, String filter, Function<String,T> parse)` that handles `>=`, `<=`, `>`, `<`, `=`, range, and equal-default. Both `Number` and date paths reuse it. For `java.util.Date`, `compareTo` is used directly (no epoch conversion) — simpler and preserves the generic shape.

- **Date format**: `MM/dd/yyyy` for both `LocalDate` and `java.util.Date`. Reason: avoids hyphen collision with range syntax (`5-10`, `01/01/2024-12/31/2024` both have exactly one `-`). `split("-", 2)` splits cleanly for both.

- **Renderer/format ignored**: filter reads `column.getValue` directly, ignoring both `format` and `renderer`. Semantic value is what the user is filtering on.

- **Filter vs. sort order**: filter first, then sort, for performance (sort only the surviving rows).

- **Hidden columns**: when a column is hidden via the column picker, drop its filter entry — mirrors the existing sort-clearing behavior in `applyColumnPicker`.

- **Sticky styling**: new filter row lives inside the existing `data-grid-header-wrapper`, so both rows scroll-stick together. No change to the wrapper itself. Filter row gets its own grid with `grid-template-columns: var(--cols)` and `min-height: var(--row-height)` to match header sizing.

- **Binding via `TextInput`**: pass the dotted signal name (e.g. `grid1.filter.quantity`) as the `name` to `TextInput`. The existing `Util.toKebabCase`/`data-signals:name` path handles it correctly because DataStar maps the attribute form back to the nested signal path. No change to `BaseInput`.

## Implementation Steps

Each step leaves the app runnable and is independently verifiable.

### Step 1: Switch `DataGrid.id` to sequential counter
**Files**: `src/main/java/com/bookie/components/DataGrid.java`
- Add `private static final AtomicInteger idCounter = new AtomicInteger(0);`
- Change `private final String id = "grid-" + UUID.randomUUID();` to `private final String id = "grid" + idCounter.getAndIncrement();`
- Remove the now-unused `UUID` import if no other usages remain.
- **Verify**: run the app, open any page with a grid, inspect DOM — grid element has `id="grid0"` (or similar). All existing functionality unchanged.

### Step 2: Add `GridFilter` class
**Files**: `src/main/java/com/bookie/components/GridFilter.java` (new)
- Public static `matches(Object cellValue, String filterText)`:
  - Return `false` if `cellValue == null`.
  - Return `true` if `filterText == null` or `filterText.isBlank()`.
  - Dispatch on `cellValue instanceof`:
    - `Number` → `matchesOrdinal(((Number) value).doubleValue(), filterText, Double::parseDouble)`
    - `Boolean` → `matchesBoolean((Boolean) value, filterText)`
    - `LocalDate` → `matchesOrdinal((LocalDate) value, filterText, text -> LocalDate.parse(text, DateTimeFormatter.ofPattern("MM/dd/yyyy")))`
    - `java.util.Date` → `matchesOrdinal((Date) value, filterText, text -> new SimpleDateFormat("MM/dd/yyyy").parse(text))`
    - else → `matchesString(value.toString(), filterText)`
- Private `matchesString(String value, String filter)`: split filter on `,`, trim each token, return true if any non-empty token is contained in `value` (case-insensitive).
- Private `matchesOrdinal(Comparable<T> value, String filter, ThrowingFunction<String,T> parse)`:
  - Strip all spaces from filter.
  - Try in order: `>=`, `<=`, `>`, `<`, `=` prefix; else if filter contains `-` and does not start with `-` → split on first `-`, parse both sides, inclusive range; else parse whole filter and test for equality via `compareTo == 0`.
  - Any parse failure → return `false`.
- Private `matchesBoolean(Boolean value, String filter)`: `"Y".equalsIgnoreCase(filter.trim())` → match `true`; `"N".equalsIgnoreCase(filter.trim())` → match `false`; otherwise `false`.
- **Verify**: no test infra in this project — confirm the class compiles and manually smoke-test after Step 4.

### Step 3: Render filter row (no behavior)
**Files**: `src/main/java/com/bookie/components/DataGrid.java`
- Add fields: `private boolean filterable = false;` and `private final Map<String, String> filters = new HashMap<>();`
- Add fluent setter `public DataGrid<TRow> filterable() { this.filterable = true; return this; }`
- In `render()`, after `var headerCells = …` block, when `filterable` is `true`, build `filterRow`:
  - Empty action cell if `hasActionColumn()` (a div with class `data-grid-th data-grid-action-th`).
  - For each visible column: a div wrapping `TextInput.textInput(this.id + ".filter." + column.getName(), filters.get(column.getName())).render()`.
  - Wrap all cells in a `<div class="data-grid-filter-row">…</div>` that uses `grid-template-columns: var(--cols)` via CSS (see Step 5).
- Emit `filterRow` inside `data-grid-header-wrapper`, directly after the existing `data-grid-header` div.
- **Verify**: enable `.filterable()` on one grid (e.g. Positions), reload page. Filter row is visible below header, aligned with columns, action column is blank, inputs are empty, typing does nothing yet. Sticky scroll covers both rows.

### Step 4: Wire filter endpoint and apply filter in render
**Files**:
- `src/main/java/com/bookie/components/DataGrid.java`
- Callers registering grid routes (grep for `DataGrid.setupRoutes` — route registration already passes through `setupRoutes`, so only that method needs the new POST)

Changes:
- In `setupRoutes`, add `.POST("filter", request -> getGrid.apply(request).applyFilter(request))`.
- Add handler:
  ```java
  private ServerResponse applyFilter(ServerRequest request) throws Exception {
      var body = request.body(new ParameterizedTypeReference<Map<String, Object>>() {});
      var gridSignals = (Map<String, Object>) body.getOrDefault(this.id, Map.of());
      var filterSignals = (Map<String, Object>) gridSignals.getOrDefault("filter", Map.of());
      this.filters.clear();
      filterSignals.forEach((columnName, rawValue) -> {
          if (rawValue != null && !rawValue.toString().isBlank()) {
              this.filters.put(columnName, rawValue.toString());
          }
      });
      this.reRender();
      return ServerResponse.ok().build();
  }
  ```
- In `render()`, introduce `var filteredRows = filterRows(this.rows);` before `sortRows` and pass `filteredRows` to `sortRows`.
- Add `private List<TRow> filterRows(List<TRow> rows)`:
  - If `filters.isEmpty()` return `rows`.
  - For each active filter entry: find column by name; detect the type from the first non-null raw value across all rows; if no non-null row exists → return `List.of()` immediately (per spec); otherwise stream-filter rows where `GridFilter.matches(column.getValue.apply(row), filterText)` is true.
  - Apply each active filter in sequence (AND across columns).
- Attach `data-on:change="@post('${endpoint}/filter')"` to each filter-row `TextInput` cell wrapper (see Step 3 — this attribute goes on the cell div).
- **Verify**:
  - Type `foo` in Name filter → grid narrows to rows whose Name contains `foo`.
  - Type `>5` on a numeric column → rows with values > 5.
  - Type `1-10` on a numeric column → inclusive range.
  - Type `01/01/2024-12/31/2024` on `lastActivity` → rows within the range.
  - Type `Y` on a boolean column → true rows only.
  - Clear the input → grid restores.
  - Sort after filtering → only filtered rows are sorted.
  - Multiple filters combine with AND.
  - Column with all null values for the filtered column yields empty list.

### Step 5: Filter row styling
**Files**: `src/main/resources/static/global-styles.css`
- Add `.data-grid-filter-row` selector near `.data-grid-header`:
  - `display: grid;`
  - `grid-template-columns: var(--cols);`
  - `min-height: var(--row-height);`
  - `background-color: var(--clr-surface);`
  - Border styling matching the header row (bottom border to visually separate from body).
  - Inputs inside fill the cell like the existing `.data-grid-cell input` rules (transparent background, no border) — consider scoping those existing rules or adding a filter-cell-specific rule.
- **Verify**: filter row is same height as the header, visually continuous with it, sticky behavior spans both rows while body scrolls, column cells align with data cells.

### Step 6: Clear filter when column hidden
**Files**: `src/main/java/com/bookie/components/DataGrid.java`
- In `applyColumnPicker`, after the `withVisible` loop, add:
  ```java
  this.filters.keySet().removeIf(columnName -> {
      var column = columns.stream().filter(c -> Objects.equals(c.getName(), columnName)).findFirst().orElse(null);
      return column == null || !column.visible;
  });
  ```
- **Verify**: apply a filter on Name → hide Name via column picker → filter cleared, all rows visible. Re-show Name → filter input is blank (no stale state).

## Out of Scope
- Per-column `filterable(false)` opt-out (only the action column is non-filterable, per spec).
- Debounced live filtering on `input` event.
- Case-sensitive string match option.
- Regex or glob patterns.
- Multiple operators in one filter (e.g. `>5,<10`).
- Locale-aware number/date parsing (using fixed `Double.parseDouble` and `MM/dd/yyyy`).
- Unit tests for `GridFilter` (project has no test infrastructure).
- Persisting filter state across sessions.
- Filter chips / summary UI.

## Open Questions
- None blocking. If issues surface during Step 4 verification with DataStar signal nesting (specifically how the attribute `data-signals:grid1.filter.columnName` is read back via `request.body()` as a nested `Map<String, Object>`), revisit the body parsing — worst case, flatten signal names instead of nesting.
