package com.bookie.infra;

import java.util.List;
import java.util.function.Function;

public record EscapedHtml(String rawHtml) implements Renderable {

    static EscapedHtml rawHtml(String html) {
        return new EscapedHtml(html != null ? html : "");
    }

    public static EscapedHtml blank() { return new EscapedHtml(""); }

    public static <T> EscapedHtml concat(List<T> items, Function<T, EscapedHtml> formatter) {
        return items.stream()
                .map(formatter)
                .reduce(EscapedHtml::concat)
                .orElse(blank());
    }

    public EscapedHtml concat(EscapedHtml other) {
        return rawHtml(rawHtml + other.rawHtml);
    }

    @Override
    public EscapedHtml render() {
        return this;
    }

    @Override
    public String toString() {
        return rawHtml;
    }

}
