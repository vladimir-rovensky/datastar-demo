package com.bookie.components;

import com.bookie.infra.ClientChannel;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static com.bookie.infra.TemplatingEngine.html;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class DataGrid<TRow> {

    private static final AtomicInteger idCounter = new AtomicInteger(0);

    public enum SortDirection { Ascending, Descending }

    private final List<DataGridColumn<TRow>> columns;
    private Function<TRow, EscapedHtml> onRowDoubleClick;
    private Function<TRow, Object> getRowID = _ -> UUID.randomUUID().toString();
    private Function<TRow, EscapedHtml> getRowAttrs = _ -> EscapedHtml.blank();
    private Function<TRow, String> rowIDSignal = _ -> "";
    private List<TRow> rows;
    private Function<TRow, EscapedHtml> getDeleteAction;
    private String deleteRowTooltip = "Delete Row";
    private EscapedHtml addRowAction;
    private String addRowTooltip = "Add Row";
    private String noRowsMessage = "Nothing here...";
    private boolean stripedRows = false;
    private String endpoint;
    private boolean showColumnPicker = false;
    private String sortColumnName;
    private SortDirection sortDirection;
    private Supplier<ClientChannel> getUpdateChannel = () -> null;
    private final String id = "grid" + idCounter.getAndIncrement();
    private boolean filterable = false;
    private final Map<String, String> filters = new HashMap<>();

    private DataGrid(List<DataGridColumn<TRow>> columns) {
        this.columns = columns;
    }

    public static <T> DataGridColumn<T> column(String header, Function<T, Object> value) {
        return new DataGridColumn<>(header, value);
    }

    @SafeVarargs
    public static <T> DataGrid<T> withColumns(DataGridColumn<T>... columns) {
        return new DataGrid<>(new ArrayList<>(List.of(columns)));
    }

    public static <T> void setupRoutes(RouterFunctions.Builder builder, Function<ServerRequest, DataGrid<T>> getGrid) {
        builder
                .GET("column-picker", request -> getGrid.apply(request).openColumnPicker())
                .POST("column-picker", request -> getGrid.apply(request).applyColumnPicker(request))
                .DELETE("column-picker", request -> getGrid.apply(request).cancelColumnPicker())
                .POST("sort/{columnName}", request -> getGrid.apply(request).applySort(request.pathVariable("columnName")))
                .POST("filter", request -> getGrid.apply(request).applyFilter(request));
    }

    public EscapedHtml render() {
        var visibleColumns = getVisibleColumns();
        var actionHeaderCell = getActionHeaderCell();
        var headerCells = EscapedHtml.concat(visibleColumns, c -> {
            if (endpoint != null) {
                return html("""
                        <div class="data-grid-th sortable" role="columnheader" data-on:click="@post('${endpoint}/sort/${columnName}')">${h}${indicator}</div>""",
                        "endpoint", endpoint,
                        "columnName", c.getName(),
                        "h", c.header,
                        "indicator", getSortIndicator(c));
            }
            return html("""
                    <div class="data-grid-th" role="columnheader">${h}</div>""", "h", c.header);
        });

        var filterRow = getFilterRow(visibleColumns);

        var displayRows = sortRows(filterRows(this.rows));

        var bodyRows = displayRows.isEmpty()
                ? html("""
                        <p class="centered-message">${message}</p>""", "message", noRowsMessage)
                : EscapedHtml.concat(displayRows, this::renderRow);
        var columnTemplate = getColumnStyleTemplate();

        var stripedClass = stripedRows ? " striped-rows" : "";

        return html("""
                <!--suppress CssInvalidFunction -->
                <div id="${id}" class="data-grid fill-height${stripedClass}" role="grid" style="--cols: ${columnTemplate}">
                    <div class="data-grid-header-wrapper">
                        <div class="data-grid-header">${actionHeader}${headers}</div>
                        ${filterRow}
                    </div>
                    <div class="data-grid-body fill-height">${rows}</div>
                </div>
                """,
                "id", this.id,
                "stripedClass", stripedClass,
                "columnTemplate", columnTemplate,
                "actionHeader", actionHeaderCell,
                "headers", headerCells,
                "filterRow", filterRow,
                "rows", bodyRows);
    }

    private ServerResponse openColumnPicker() {
        if (endpoint == null) {
            throw new RuntimeException("DataGrid endpoint not set.");
        }
        var allHeaders = this.columns.stream().map(c -> c.header).toList();
        var visibleHeaders = getVisibleColumns().stream().map(c -> c.header).toList();
        var pickerPath = endpoint + "/column-picker";
        var content = Popup.popup()
                .withTitle("Visible Columns")
                .withContent(html("""
                        <div class="column-picker-content">${multiselect}</div>
                        """, "multiselect", MultiselectInput.multiselectInput("visibleColumns", allHeaders, visibleHeaders).render()))
                .withActions(html("""
                        <button class="btn-primary" data-on:click="@post('${path}')">OK</button>
                        <button data-on:click="@delete('${path}')">Cancel</button>
                        """, "path", pickerPath))
                .render();
        return Popup.open(content);
    }

    @SuppressWarnings("unchecked")
    private ServerResponse applyColumnPicker(ServerRequest request) throws Exception {
        var body = request.body(new ParameterizedTypeReference<Map<String, Object>>() {});
        var visibleColumns = (List<String>) body.getOrDefault("visibleColumns", List.of());
        this.columns.forEach(c -> c.withVisible(visibleColumns.contains(c.header)));

        var sortColumn = getSortColumn();
        if (sortColumn != null && !sortColumn.visible) {
            clearSort();
        }

        this.filters.keySet().removeIf(columnName ->
                getColumnByName(columnName).map(c -> !c.visible).orElse(true));

        this.reRender();
        return Popup.close();
    }

    private ServerResponse cancelColumnPicker() {
        return Popup.close();
    }

    private ServerResponse applySort(String columnName) {
        var matchingColumn = getColumnByName(columnName);

        if (matchingColumn.isEmpty()) {
            return ServerResponse.ok().build();
        }

        if (!Objects.equals(sortColumnName, columnName)) {
            sortColumnName = columnName;
            sortDirection = SortDirection.Ascending;
        } else if (sortDirection == SortDirection.Ascending) {
            sortDirection = SortDirection.Descending;
        } else {
            clearSort();
        }

        reRender();
        return ServerResponse.ok().build();
    }

    @SuppressWarnings("unchecked")
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

    private EscapedHtml getFilterRow(List<DataGridColumn<TRow>> visibleColumns) {
        if (!filterable) {
            return EscapedHtml.blank();
        }

        var filterActionCell = hasActionColumn()
                ? html("""
                        <div class="data-grid-th data-grid-action-th">&#x2315;</div>""")
                : EscapedHtml.blank();

        var filterCells = EscapedHtml.concat(visibleColumns, column -> html("""
                <div class="data-grid-filter-cell" data-on:change="@post('${endpoint}/filter')">${input}</div>""",
                "endpoint", endpoint,
                "input", TextInput.textInput(this.id + ".filter." + column.getName(), filters.get(column.getName()))));

        return html("""
                <div class="data-grid-filter-row">${actionCell}${cells}</div>""",
                "actionCell", filterActionCell,
                "cells", filterCells);
    }

    private List<TRow> filterRows(List<TRow> rows) {
        if (filters.isEmpty()) {
            return rows;
        }

        var predicate = filters.entrySet().stream()
                .map(entry -> getColumnByName(entry.getKey())
                        .<Predicate<TRow>>map(column -> {
                            var filterText = entry.getValue();
                            return row -> GridFilter.matches(column.getValue.apply(row), filterText);
                        })
                        .orElse(_ -> true))
                .reduce(Predicate::and)
                .orElse(_ -> true);

        return rows.stream().filter(predicate).toList();
    }

    private EscapedHtml getSortIndicator(DataGridColumn<TRow> column) {
        if (!Objects.equals(column.getName(), sortColumnName)) {
            return EscapedHtml.blank();
        }
        return html("""
                <span class="data-grid-sort-indicator">${glyph}</span>""",
                "glyph", sortDirection == SortDirection.Ascending ? "▲" : "▼");
    }

    private List<TRow> sortRows(List<TRow> displayRows) {

        var sortColumn = getSortColumn();

        if (sortColumn == null) {
            return displayRows;
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        Comparator<TRow> comparator = Comparator.comparing(
                row -> (Comparable) sortColumn.getValue.apply(row),
                Comparator.nullsLast(Comparator.naturalOrder()));

        if (sortDirection == SortDirection.Descending) {
            comparator = comparator.reversed();
        }

        return displayRows.stream().sorted(comparator).toList();
    }

    private @NotNull String getColumnStyleTemplate() {
        return hasActionColumn()
                ? "40px repeat(" + getVisibleColumns().size() + ", minmax(150px, 1fr))"
                : "repeat(" + getVisibleColumns().size() + ", minmax(150px, 1fr))";
    }

    private EscapedHtml getActionHeaderCell() {
        if (!hasActionColumn()) {
            return EscapedHtml.blank();
        }

        if (addRowAction != null) {
            return html("""
                    <div class="data-grid-th data-grid-action-th" role="columnheader">
                        <button class="btn-no-bg" data-on:click="${action}" data-tooltip='${tooltip}'>+</button>
                    </div>""",
                    "action", addRowAction,
                    "tooltip", addRowTooltip);
        }

        if (showColumnPicker) {
            if (endpoint == null) {
                throw new RuntimeException("DataGrid endpoint not set.");
            }
            return html("""
                    <div class="data-grid-th data-grid-action-th" role="columnheader">
                        <button class="column-picker-btn btn-no-bg" data-on:click="@get('${path}/column-picker')" data-tooltip="Pick columns">≡</button>
                    </div>""",
                    "path", endpoint);
        }

        return html("""
                <div class="data-grid-th data-grid-action-th" role="columnheader"></div>""");
    }

    private EscapedHtml renderRow(TRow row) {
        var actionCell = hasActionColumn()
                ? (getDeleteAction != null
                   ? renderDeleteCell(row)
                    : html("""
                        <div id="${cellID}" class="data-grid-cell data-grid-action-cell" role="gridcell"></div>""",
                        "cellID", getRowID.apply(row) + "-actionCell"))
                : EscapedHtml.blank();

        var cells = EscapedHtml.concat(getVisibleColumns(), c -> renderCell(row, c));

        var dblClick = onRowDoubleClick != null
                ? " data-on:dblclick=" + onRowDoubleClick.apply(row)
                : "";

        var attrs = getRowAttrs.apply(row);

        var id = getRowID.apply(row);
        var idSignalAttr = getIdSignalAttr(row, id);

        return html("""
                <div class="data-grid-row" id="${rowID}" role="row" ${dblClick} ${rowIDSignal} ${attrs}>${actionCell}${cells}</div>""",
                "rowID", id,
                "dblClick", dblClick,
                "rowIDSignal", idSignalAttr,
                "attrs", attrs,
                "actionCell", actionCell,
                "cells", cells);
    }

    private EscapedHtml renderDeleteCell(TRow row) {
        return html("""
                <div id="${cellID}" class="data-grid-cell data-grid-action-cell" role="gridcell"><button class="btn-no-bg" data-on:click="${action}" data-tooltip='${tooltip}'>✕</button></div>""",
                "cellID", getRowID.apply(row) + "-deleteRowBtn",
                "action", getDeleteAction.apply(row),
                "tooltip", deleteRowTooltip);
    }

    private @NotNull EscapedHtml renderCell(TRow row, DataGridColumn<TRow> column) {
        Object displayValue = getDisplayValue(row, column);
        return html("""
                <div id="${cellID}" class="data-grid-cell" role="gridcell">${value}</div>""",
                "cellID", getRowID.apply(row) + "-" + column.getName(),
                "value", displayValue);
    }

    @SuppressWarnings("unchecked")
    private Object getDisplayValue(TRow row, DataGridColumn<TRow> column) {
        Object displayValue;
        if (column.renderer != null) {
            displayValue = column.renderer.apply(row);
        } else {
            var rawValue = column.getValue.apply(row);
            displayValue = column.format.apply(rawValue);
        }

        return displayValue;
    }

    private @NotNull EscapedHtml getIdSignalAttr(TRow row, Object id) {
        var idSignal = rowIDSignal.apply(row);

        return idSignal.isBlank()
                ? EscapedHtml.blank()
                : html("data-signals:${signal}=\"${rowID}\"", "signal", Util.toKebabCase(idSignal), "rowID", Util.toJson(id));
    }

    private @Nullable DataGridColumn<TRow> getSortColumn() {
        if (sortColumnName == null) {
            return null;
        }

        return getColumnByName(sortColumnName).orElse(null);
    }

    private boolean hasActionColumn() {
        return addRowAction != null || getDeleteAction != null || showColumnPicker;
    }

    private List<DataGridColumn<TRow>> getVisibleColumns() {
        return columns.stream().filter(c -> c.visible).toList();
    }

    private Optional<DataGridColumn<TRow>> getColumnByName(String columnName) {
        return columns.stream()
                .filter(c -> c.getName().equalsIgnoreCase(columnName))
                .findFirst();
    }

    private void clearSort() {
        sortColumnName = null;
        sortDirection = null;
    }

    private void reRender() {
        var updateChannel = getUpdateChannel.get();

        if (updateChannel == null) {
            throw new RuntimeException("Grid update channel not set.");
        }

        updateChannel.updateFragment(this.render());
    }

    public List<DataGridColumn<TRow>> getColumns() {
        return this.columns;
    }

    public DataGrid<TRow> columns(List<DataGridColumn<TRow>> additionalColumns) {
        this.columns.addAll(additionalColumns);
        return this;
    }

    public DataGrid<TRow> onRowDoubleClick(Function<TRow, EscapedHtml> action) {
        this.onRowDoubleClick = action;
        return this;
    }

    public DataGrid<TRow> withRowAttrs(Function<TRow, EscapedHtml> getRowAttrs) {
        this.getRowAttrs = getRowAttrs;
        return this;
    }

    public DataGrid<TRow> withRowID(Function<TRow, Object> getID) {
        this.getRowID = getID;
        return this;
    }

    public DataGrid<TRow> withRows(List<TRow> rows) {
        this.rows = rows;
        return this;
    }

    public DataGrid<TRow> withRowIDSignal(Function<TRow, String> rowIDSignal) {
        this.rowIDSignal = rowIDSignal;
        return this;
    }

    public DataGrid<TRow> onDeleteRow(Function<TRow, EscapedHtml> getDeleteAction) {
        this.getDeleteAction = getDeleteAction;
        return this;
    }

    public DataGrid<TRow> withDeleteRowTooltip(String deleteRowTooltip) {
        this.deleteRowTooltip = deleteRowTooltip;
        return this;
    }

    public DataGrid<TRow> onAddRow(EscapedHtml addRowAction) {
        this.addRowAction = addRowAction;
        return this;
    }

    public DataGrid<TRow> withAddRowTooltip(String addRowTooltip) {
        this.addRowTooltip = addRowTooltip;
        return this;
    }

    public DataGrid<TRow> withNoRowsMessage(String noRowsMessage) {
        this.noRowsMessage = noRowsMessage;
        return this;
    }

    public DataGrid<TRow> withStripedRows() {
        this.stripedRows = true;
        return this;
    }

    public DataGrid<TRow> withEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }

    public DataGrid<TRow> withColumnPicker() {
        this.showColumnPicker = true;
        return this;
    }

    public DataGrid<TRow> withUpdateChannel(Supplier<ClientChannel> getUpdateChannel) {
        this.getUpdateChannel = getUpdateChannel;
        return this;
    }

    public DataGrid<TRow> filterable() {
        this.filterable = true;
        return this;
    }

}
