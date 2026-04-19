# Feature: DataGrid dataView refactor

## Goal
Stop re-sorting on every render. DataGrid keeps a `dataView` that is rebuilt only when source rows or sort state change. TradesScreen and PositionsScreen switch to screen-specific row records and replace their `.reversed()` ordering with an explicit initial sort.

## Key Decisions
- **`dataView` is `List<TRow>`, not a pre-formatted structure.** Sort still needs comparable raw values; format/render continues to run per cell on `render()`. Avoids duplicated state and keeps the data model simple.
- **Rebuild only on `withRows()` and `applySort()`.** Column picker does not touch `dataView`; hiding a sorted column just hides its indicator, sort state is retained.
- **`reRender()` stays private.** Grid-owned endpoints (sort, column picker) push via the update channel; external data changes are pushed by the parent screen using its own channel.
- **`TradeScreenRow(Trade, Bond)` / `PositionScreenRow(Position, Bond)` records with inline column lambdas.** No custom getters. `CommonColumns.bondColumns(...)` is unchanged; callers pass `r -> Optional.ofNullable(r.bond())`.

## Implementation Steps

1. **DataGrid: add `dataView` and `rebuildDataView()`.** Rename field `rows` → `sourceRows`. Add `private List<TRow> dataView`. Private `rebuildDataView()` assigns `sortRows(sourceRows)`, or `List.copyOf(sourceRows)` when sort is unset. Call it from `withRows()` and `applySort()`. `render()` iterates `dataView`; drop the per-render `sortRows` call. `sortRows` stays as the sort helper.

2. **DataGrid: simplify `applyColumnPicker`.** Remove the `clearSort()`-on-hide branch. Just update visibility and call `reRender()`. No `dataView` work here.

3. **DataGrid: add `withSort(String columnName, SortDirection direction)`.** Assigns `sortColumnName`/`sortDirection`. If `sourceRows` is non-null, also calls `rebuildDataView()` so order of `withSort`/`withRows` in the builder does not matter. Unknown column name: silent no-op.

4. **TradesScreen: migrate to `TradeScreenRow`.** Add record `TradeScreenRow(Trade trade, Bond bond)`. Retype grid to `DataGrid<TradeScreenRow>` and rewrite column lambdas inline (e.g., `r -> r.trade().getQuantity()`). Pass `r -> Optional.ofNullable(r.bond())` to `CommonColumns.bondColumns`. Add `.withSort("ID", SortDirection.Descending)`; drop `.reversed()` and `.withRows(...)` from `getContent()` (so it just calls `tradeGrid.render()`). Add private `refreshGridRows()` that builds the list from `trades` + `bondByCusip` and calls `tradeGrid.withRows(...)`; invoke it from the constructor and from each event handler before `triggerUpdate()` (including the async bond-load callback in `loadBondsFor`). Remove `getBond(String)` if unused afterward.

5. **PositionsScreen: analogous migration.** Add record `PositionScreenRow(Position position, Bond bond)`. Retype grid and rewrite lambdas inline. Add `.withSort("<lastActivityColumnName>", SortDirection.Descending)`. Add `refreshGridRows()`; invoke from constructor and each event handler; drop any manual reverse/sort from `getContent()`.

## Out of Scope
- Filtering (design leaves room for it in `rebuildDataView`, but no implementation).
- Formatted/display value caching — values still computed per render.
- Surgical updates to screen row lists; refresh is always a full rebuild.
- SecuritiesScreen — its per-render `.withRows()` pattern remains valid.

## Open Questions
- Exact `DataGridColumn.getName()` value for the Last Activity column in PositionsScreen — confirm when implementing step 5.
- Whether `getBond(String)` in TradesScreen has callers beyond the migrated column lambdas; remove only if fully unused.