package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import org.jetbrains.annotations.NotNull;

import static com.bookie.infra.TemplatingEngine.html;

import java.util.List;
import java.util.function.Function;

public class DataGrid<TRow> {

    public static <T> DataGridColumn<T> column(String header, Function<T, Object> value) {
        return new DataGridColumn<>(header, value);
    }

    private final List<DataGridColumn<TRow>> columns;
    private Function<TRow, EscapedHtml> onRowDoubleClick;
    private Function<TRow, Object> getRowID = _ -> null;
    private Function<TRow, EscapedHtml> getRowAttrs = _ -> EscapedHtml.blank();
    private List<TRow> rows;

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

    public EscapedHtml render() {
        var headerCells = EscapedHtml.concat(columns, c -> html("""
                <div class="data-grid-th">${h}</div>""", "h", c.header));
        var bodyRows = EscapedHtml.concat(rows, this::renderRow);

        return html("""
                <div class="data-grid fill-height" style="--cols: repeat(${columnCount}, 1fr)">
                    <div class="data-grid-header">${headers}</div>
                    <div class="data-grid-body fill-height">${rows}</div>
                </div>
                """,
                "columnCount", columns.size(),
                "headers", headerCells,
                "rows", bodyRows);
    }

    private EscapedHtml renderRow(TRow row) {
        var cells = EscapedHtml.concat(columns, c -> renderCell(row, c));

        var dblClick = onRowDoubleClick != null
                ? " data-on:dblclick=" + onRowDoubleClick.apply(row)
                : "";

        var attrs = getRowAttrs.apply(row);

        var id = getRowID.apply(row);

        return html("""
                <div class="data-grid-row" id="${rowID}" ${dblClick} ${attrs}>${cells}</div>""",
                "rowID", id != null ? id : "",
                "dblClick", dblClick,
                "attrs", attrs,
                "cells", cells);
    }

    private @NotNull EscapedHtml renderCell(TRow row, DataGridColumn<TRow> column) {
        return html("""
                <div class="data-grid-cell">${value}</div>""",
                "value",
                column.renderer != null
                        ? column.renderer.apply(row)
                        : column.getValue.apply(row));
    }

}
