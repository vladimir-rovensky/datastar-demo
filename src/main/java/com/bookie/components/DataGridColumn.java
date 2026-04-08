package com.bookie.components;

import java.util.function.Function;

public class DataGridColumn<TRow> {
    String header;
    Function<TRow, Object> getValue;

    DataGridColumn(String header, Function<TRow, Object> getValue) {
        this.header = header;
        this.getValue = getValue;
    }
}
