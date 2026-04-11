package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

import java.util.function.Function;

public class DataGridColumn<TRow> {
    String header;
    Function<TRow, Object> getValue;
    Function<TRow, Renderable> renderer;

    DataGridColumn(String header, Function<TRow, Object> getValue) {
        this.header = header;
        this.getValue = getValue;
    }

    public DataGridColumn<TRow> withRenderer(Function<TRow, Renderable> renderer) {
        this.renderer = renderer;
        return this;
    }
}
