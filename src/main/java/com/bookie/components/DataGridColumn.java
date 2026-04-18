package com.bookie.components;

import com.bookie.infra.Renderable;

import java.util.function.Function;

public class DataGridColumn<TRow> {
    public String header;
    public Function<TRow, Object> getValue;
    public Function<TRow, Renderable> renderer;
    public boolean visible = true;

    DataGridColumn(String header, Function<TRow, Object> getValue) {
        this.header = header;
        this.getValue = getValue;
    }

    public DataGridColumn<TRow> withRenderer(Function<TRow, Renderable> renderer) {
        this.renderer = renderer;
        return this;
    }

    public DataGridColumn<TRow> withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public String getName() {
        return header.replace(" ", "_");
    }
}
