package com.bookie.components;

import com.bookie.infra.Format;
import com.bookie.infra.Renderable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;
import java.util.function.Function;

public class DataGridColumn<TRow> {
    public String header;
    public Function<TRow, Object> getValue;
    public Function<TRow, Renderable> renderer;
    @SuppressWarnings("rawtypes")
    public Function format = DataGridColumn::defaultFormat;

    public boolean visible = true;

    DataGridColumn(String header, Function<TRow, Object> getValue) {
        this.header = header;
        this.getValue = getValue;
    }

    public DataGridColumn<TRow> withRenderer(Function<TRow, Renderable> renderer) {
        this.renderer = renderer;
        return this;
    }

    public <T> DataGridColumn<TRow> withFormat(Function<T, String> format) {
        this.format = format;
        return this;
    }

    public DataGridColumn<TRow> withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public String getName() {
        return header.toLowerCase().replace("/", "_").replaceAll("[^a-z0-9]", "_");
    }

    private static String defaultFormat(Object value) {
        return switch (value) {
            case null -> "";
            case BigDecimal decimal -> Format.decimal(decimal);
            case LocalDate date -> Format.usDate(date);
            case Date date -> Format.usDate(date);
            case Boolean bool -> Format.bool(bool);
            default -> value.toString();
        };
    }
}
