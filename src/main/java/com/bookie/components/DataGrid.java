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

import static com.bookie.infra.HtmlExtensions.X;
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
    public static final int ROW_HEIGHT_PX = 36;

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
    private final VirtualScrollManager virtualScrollManager = new VirtualScrollManager(ROW_HEIGHT_PX);

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
                .PUT("column-picker", request -> getGrid.apply(request).applyColumnPicker(request))
                .DELETE("column-picker", request -> getGrid.apply(request).cancelColumnPicker())
                .PUT("sort/{columnName}", request -> getGrid.apply(request).applySort(request.pathVariable("columnName")))
                .PUT("filter", request -> getGrid.apply(request).applyFilter(request))
                .PUT("viewport", request -> getGrid.apply(request).applyViewport(request));
    }

    public synchronized EscapedHtml render() {
        var visibleColumns = getVisibleColumns();
        var actionHeaderCell = getActionHeaderCell();
        var headerCells = EscapedHtml.concat(visibleColumns, c -> {
            if (endpoint != null) {
                return html("""
                        <div class="data-grid-th sortable" role="columnheader" data-on:click="${sortAction}">${h}${indicator}</div>""",
                        "sortAction", X.put(endpoint + "/sort/" + c.getName()),
                        "h", c.header,
                        "indicator", getSortIndicator(c));
            }
            return html("""
                    <div class="data-grid-th" role="columnheader">${h}</div>""", "h", c.header);
        });

        var filterRow = getFilterRow(visibleColumns);

        var displayRows = sortRows(filterRows(List.copyOf(this.rows)));

        var bodyRows = buildBodyRows(displayRows);
        var columnTemplate = getColumnStyleTemplate();

        var stripedClass = stripedRows ? " striped-rows" : "";
        var scrollHandler = endpoint != null
                ? html("""
                        data-on:scroll__throttle.50ms.trailing="if(el.prevScrollTop !== undefined && el.prevScrollTop !== el.scrollTop) { ${scrollAction}; } el.prevScrollTop = el.scrollTop;"
                        """,
                        "scrollAction", X.put(endpoint + "/viewport")
                                .withPayload("{scrollTop: el.scrollTop, viewportHeight: el.clientHeight}"))
                : EscapedHtml.blank();

        var resizeHandler = endpoint != null
                ? html("""
                        data-on:data-grid-viewport-resize__debounce_150="${resizeAction}"
                        """,
                        "resizeAction", X.put(endpoint + "/viewport")
                                .withPayload("{scrollTop: el.scrollTop, viewportHeight: el.clientHeight}"))
                : EscapedHtml.blank();

        var resizeScript = getResizeObserverScript();

        return html("""
                <!--suppress CssInvalidFunction -->
                <div id="${id}" class="data-grid fill-height${stripedClass}" role="grid" style="--cols: ${columnTemplate}" ${scrollHandler} ${resizeHandler}>
                    <div class="data-grid-header-wrapper">
                        <div class="data-grid-header">${actionHeader}${headers}</div>
                        ${filterRow}
                    </div>
                    <div id="${bodyId}" class="data-grid-body fill-height">${resizeScript}${rows}</div>
                </div>
                """,
                "id", this.id,
                "bodyId", this.id + "-body",
                "stripedClass", stripedClass,
                "columnTemplate", columnTemplate,
                "scrollHandler", scrollHandler,
                "resizeHandler", resizeHandler,
                "actionHeader", actionHeaderCell,
                "headers", headerCells,
                "filterRow", filterRow,
                "resizeScript", resizeScript,
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
                        <button class="btn-primary" data-on:click="${okAction}">OK</button>
                        <button data-on:click="${cancelAction}">Cancel</button>
                        """,
                        "okAction", X.put(pickerPath),
                        "cancelAction", X.delete(pickerPath)))
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

    private ServerResponse applyViewport(ServerRequest request) throws Exception {
        var body = request.body(new ParameterizedTypeReference<Map<String, Object>>() {});
        virtualScrollManager.updateViewport(
                ((Number) body.getOrDefault("scrollTop", 0)).intValue(),
                ((Number) body.getOrDefault("viewportHeight", 0)).intValue());
        this.reRender();
        return ServerResponse.ok().build();
    }

    private EscapedHtml getResizeObserverScript() {
        if (endpoint == null) {
            return EscapedHtml.blank();
        }

        return html("""
                <script>
                (function() {
                    let scrollBody = document.currentScript.parentElement;
                    let scrollContainer = scrollBody.parentElement;
                    if (scrollContainer.resizeObserverInitialized) return;
                    scrollContainer.resizeObserverInitialized = true;
                    new ResizeObserver(() => { scrollContainer.dispatchEvent(new CustomEvent('data-grid-viewport-resize')); })
                        .observe(scrollContainer);
                })();
                </script>""");
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
                <div class="data-grid-filter-cell" data-on:change="${filterAction}">${input}</div>""",
                "filterAction", X.put(endpoint + "/filter"),
                "input", TextInput.textInput(this.id + ".filter." + column.getName(), filters.get(column.getName()))));

        return html("""
                <div class="data-grid-filter-row" aria-label="Filter Row">${actionCell}${cells}</div>""",
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
                        <button class="btn-no-bg" data-on:click="${action}" data-tooltip='${tooltip}' aria-label='Add Row'>+</button>
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
                        <button class="column-picker-btn btn-no-bg" data-on:click="${pickerAction}" data-tooltip="Pick columns">≡</button>
                    </div>""",
                    "pickerAction", X.get(endpoint + "/column-picker"));
        }

        return html("""
                <div class="data-grid-th data-grid-action-th" role="columnheader"></div>""");
    }

    private EscapedHtml buildBodyRows(List<TRow> allRows) {
        var window = endpoint == null
                ? new int[]{0, allRows.size()}
                : virtualScrollManager.computeWindow(allRows.size());
        return buildBodyRows(allRows, window[0], window[1]);
    }

    private EscapedHtml buildBodyRows(List<TRow> allRows, int startIndex, int endIndex) {
        if (allRows.isEmpty() || startIndex >= endIndex) {
            return html("""
                    <p class="centered-message">${message}</p>""", "message", noRowsMessage);
        }

        var rowIndex = new AtomicInteger(startIndex);
        var rowsHtml = EscapedHtml.concat(allRows.subList(startIndex, endIndex), row -> renderRow(row, rowIndex.getAndIncrement()));

        //noinspection CssInvalidPropertyValue
        return html("""
                <div id="${contentId}" class="data-grid-body-content" style="height: ${height}px">${rows}</div>""",
                "contentId", this.id + "-body-content",
                "height", allRows.size() * ROW_HEIGHT_PX,
                "rows", rowsHtml);
    }

    private EscapedHtml renderRow(TRow row, int rowIndex) {
        var actionCell = hasActionColumn()
                ? (getDeleteAction != null
                   ? renderDeleteCell(row)
                    : html("""
                        <div id="${cellID}" class="data-grid-cell data-grid-action-cell" role="gridcell"></div>""",
                        "cellID", getRowID.apply(row) + "-actionCell"))
                : EscapedHtml.blank();

        var cells = EscapedHtml.concat(getVisibleColumns(), c -> renderCell(row, c));

        var dblClick = onRowDoubleClick != null
                ? html("""
                     data-on:dblclick="${handler}"
                     """, "handler", onRowDoubleClick.apply(row))
                : EscapedHtml.blank();

        var attrs = getRowAttrs.apply(row);

        var id = getRowID.apply(row);
        var idSignalAttr = getIdSignalAttr(row, id);

        //noinspection CssInvalidPropertyValue,CssInvalidFunction
        return html("""
                <div class="data-grid-row" id="${rowID}" role="row" style="position: absolute; top: 0; width: 100%; height: ${rowHeight}px; transform: translateY(${translateY}px)" ${dblClick} ${rowIDSignal} ${attrs}>${actionCell}${cells}</div>""",
                "rowID", id,
                "rowHeight", ROW_HEIGHT_PX,
                "translateY", rowIndex * ROW_HEIGHT_PX,
                "dblClick", dblClick,
                "rowIDSignal", idSignalAttr,
                "attrs", attrs,
                "actionCell", actionCell,
                "cells", cells);
    }

    private EscapedHtml renderDeleteCell(TRow row) {
        return html("""
                <div id="${cellID}" class="data-grid-cell data-grid-action-cell" role="gridcell">
                    <button class="btn-no-bg" data-on:click="${action}" data-tooltip='${tooltip}' aria-label="Delete Row">✕</button>
                </div>""",
                "cellID", getRowID.apply(row) + "-deleteRowBtn",
                "action", getDeleteAction.apply(row),
                "tooltip", deleteRowTooltip);
    }

    private @NotNull EscapedHtml renderCell(TRow row, DataGridColumn<TRow> column) {
        Object displayValue = getDisplayValue(row, column);
        return html("""
                <div id="${cellID}" class="data-grid-cell" role="gridcell">${value}</div>""",
                "cellID", getCellId(row, column),
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

    private Optional<DataGridColumn<TRow>> getColumnByHeader(String header) {
        return columns.stream()
                .filter(c -> Objects.equals(c.header, header))
                .findFirst();
    }

    private String getCellId(TRow row, DataGridColumn<TRow> column) {
        return getRowID.apply(row) + "-" + column.getName();
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

    public DataGrid<TRow> handleInitialRender() {
        virtualScrollManager.reset();
        return this;
    }

    public void animateCell(TRow row, String columnHeader, String keyframes, int duration) {
        var channel = getUpdateChannel.get();
        if (channel == null) {
            return;
        }

        var column = getColumnByHeader(columnHeader)
                .orElseThrow(() -> new IllegalArgumentException("Column not found: " + columnHeader));

        var cellId = getCellId(row, column);

        channel.executeScript(html("""
                (function() {
                    var el = document.getElementById('${cellId}');
                    if (el) {
                        el?.anim?.cancel?.();
                        el.anim = el.animate(${keyframes}, {duration: ${duration}});
                    }
                })();
                """,
                "cellId", html(cellId),
                "keyframes", html(keyframes),
                "duration", duration).toString());
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

    public DataGrid<TRow> withDefaultSort(String sortColumnName, SortDirection direction) {
        this.sortColumnName = sortColumnName;
        this.sortDirection = direction;
        return this;
    }

}
