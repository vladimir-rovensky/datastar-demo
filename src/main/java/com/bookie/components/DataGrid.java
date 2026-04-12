package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Util;
import org.jetbrains.annotations.NotNull;

import static com.bookie.infra.TemplatingEngine.html;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public class DataGrid<TRow> {

    public static <T> DataGridColumn<T> column(String header, Function<T, Object> value) {
        return new DataGridColumn<>(header, value);
    }

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

    private DataGrid(List<DataGridColumn<TRow>> columns) {
        this.columns = columns;
    }

    @SafeVarargs
    public static <T> DataGrid<T> withColumns(DataGridColumn<T>... columns) {
        return new DataGrid<>(List.of(columns));
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

    public EscapedHtml render() {
        var actionHeaderCell = getActionHeaderCell();
        var headerCells = EscapedHtml.concat(columns, c -> html("""
                <div class="data-grid-th">${h}</div>""", "h", c.header));
        var bodyRows = rows.isEmpty()
                ? html("""
                        <p class="centered-message">${message}</p>""", "message", noRowsMessage)
                : EscapedHtml.concat(rows, this::renderRow);
        var columnTemplate = getColumnStyleTemplate();

        return html("""
                <!--suppress CssInvalidFunction -->
                <div class="data-grid fill-height" style="--cols: ${columnTemplate}">
                    <div class="data-grid-header">${actionHeader}${headers}</div>
                    <div class="data-grid-body fill-height">${rows}</div>
                </div>
                """,
                "columnTemplate", columnTemplate,
                "actionHeader", actionHeaderCell,
                "headers", headerCells,
                "rows", bodyRows);
    }

    private @NotNull String getColumnStyleTemplate() {
        return hasActionColumn()
                ? "40px repeat(" + columns.size() + ", 1fr)"
                : "repeat(" + columns.size() + ", 1fr)";
    }

    private boolean hasActionColumn() {
        return addRowAction != null || getDeleteAction != null;
    }

    private EscapedHtml getActionHeaderCell() {
        if (!hasActionColumn()) {
            return EscapedHtml.blank();
        }

        if (addRowAction != null) {
            return html("""
                    <div class="data-grid-th data-grid-action-th"><button class="btn-no-bg" data-on:click="${action}" data-tooltip='${tooltip}'>+</button></div>""",
                    "action", addRowAction,
                    "tooltip", addRowTooltip);
        }

        return html("""
                <div class="data-grid-th"></div>""");
    }

    private EscapedHtml renderRow(TRow row) {
        var deleteCell = getDeleteAction != null
                ? renderDeleteCell(row)
                : EscapedHtml.blank();
        var cells = EscapedHtml.concat(columns, c -> renderCell(row, c));

        var dblClick = onRowDoubleClick != null
                ? " data-on:dblclick=" + onRowDoubleClick.apply(row)
                : "";

        var attrs = getRowAttrs.apply(row);

        var id = getRowID.apply(row);
        var idSignalAttr = getIdSignalAttr(row, id);

        return html("""
                <div class="data-grid-row" id="${rowID}" ${dblClick} ${rowIDSignal} ${attrs}>${deleteCell}${cells}</div>""",
                "rowID", id,
                "dblClick", dblClick,
                "rowIDSignal", idSignalAttr,
                "attrs", attrs,
                "deleteCell", deleteCell,
                "cells", cells);
    }

    private EscapedHtml renderDeleteCell(TRow row) {
        return html("""
                <div id="${cellID}" class="data-grid-cell"><button class="btn-no-bg" data-on:click="${action}" data-tooltip='${tooltip}'>✕</button></div>""",
                "cellID", getRowID.apply(row) + "-deleteRowBtn",
                "action", getDeleteAction.apply(row),
                "tooltip", deleteRowTooltip);
    }

    private @NotNull EscapedHtml getIdSignalAttr(TRow row, Object id) {
        var idSignal = rowIDSignal.apply(row);

        return idSignal.isBlank()
                ? EscapedHtml.blank()
                : html("data-signals:${signal}=\"${rowID}\"", "signal", Util.toKebabCase(idSignal), "rowID", Util.toJson(id));
    }

    private @NotNull EscapedHtml renderCell(TRow row, DataGridColumn<TRow> column) {
        return html("""
                <div id="${cellID}" class="data-grid-cell">${value}</div>""",
                "cellID", getRowID.apply(row) + "-" + column.getName(),
                "value",
                column.renderer != null
                        ? column.renderer.apply(row)
                        : column.getValue.apply(row));
    }

}
