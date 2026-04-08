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
    Function<TRow, Object> getRowID = _ -> null;
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

    public DataGrid<TRow> withRowID(Function<TRow, Object> getID) {
        this.getRowID = getID;
        return this;
    }

    public DataGrid<TRow> withRows(List<TRow> rows) {
        this.rows = rows;
        return this;
    }

    public EscapedHtml render() {
        var headers = EscapedHtml.concat(columns, c -> html("<th>${h}</th>", "h", c.header));
        var bodyRows = EscapedHtml.concat(rows, this::renderRow);

        return html("""
                <table>
                    <thead><tr>${headers}</tr></thead>
                    <tbody>${rows}</tbody>
                </table>
                """,
                "headers", headers,
                "rows", bodyRows);
    }

    private EscapedHtml renderRow(TRow row) {
        var cells = EscapedHtml.concat(columns, c -> renderCell(row, c));

        var dblClick = onRowDoubleClick != null
                ? " data-on:dblclick=" + onRowDoubleClick.apply(row)
                : "";

        var id = getRowID.apply(row);

        return html("<tr id='${rowID}' ${dblClick}>${cells}</tr>",
                "rowID", id != null ? id : "",
                "dblClick", dblClick,
                "cells", cells);
    }

    private @NotNull EscapedHtml renderCell(TRow row, DataGridColumn<TRow> column) {
        return html("<td>${value}</td>", "value", column.getValue.apply(row));
    }

}
