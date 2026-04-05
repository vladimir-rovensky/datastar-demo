package com.bookie.components;

import com.bookie.infra.EscapedHtml;

import static com.bookie.infra.TemplatingEngine.html;

import java.util.List;
import java.util.function.Function;

public class DataGrid<T> {

    public record Column<T>(String header, Function<T, Object> value) {}

    public static <T> Column<T> column(String header, Function<T, Object> value) {
        return new Column<>(header, value);
    }

    private final List<Column<T>> columns;
    private Function<T, String> onRowDoubleClick;
    private List<T> rows;

    private DataGrid(List<Column<T>> columns) {
        this.columns = columns;
    }

    @SafeVarargs
    public static <T> DataGrid<T> withColumns(Column<T>... columns) {
        return new DataGrid<>(List.of(columns));
    }

    public DataGrid<T> onRowDoubleClick(Function<T, String> action) {
        this.onRowDoubleClick = action;
        return this;
    }

    public DataGrid<T> withRows(List<T> rows) {
        this.rows = rows;
        return this;
    }

    public EscapedHtml render() {
        var headers = EscapedHtml.concat(columns, c -> html("<th>${h}</th>", "h", c.header()));
        var bodyRows = EscapedHtml.concat(rows, this::renderRow);

        return html("""
                <table>
                    <thead><tr>@{headers}</tr></thead>
                    <tbody>@{rows}</tbody>
                </table>
                """,
                "headers", headers,
                "rows", bodyRows);
    }

    private EscapedHtml renderRow(T row) {
        var cells = EscapedHtml.concat(columns, c -> html("<td>${v}</td>", "v", c.value().apply(row)));

        var dblClick = onRowDoubleClick != null
                ? " data-on:dblclick=\"" + onRowDoubleClick.apply(row) + "\""
                : "";

        return html("<tr" + dblClick + ">@{cells}</tr>", "cells", cells);
    }
}
