package com.bookie.infra;

public record EscapedHtml(String html) implements Renderable {

    public static EscapedHtml html(String html) {
        return new EscapedHtml(html);
    }

    public static EscapedHtml blank() { return new EscapedHtml(""); }

    @Override
    public EscapedHtml render() {
        return this;
    }

    @Override
    public String toString() {
        return html;
    }

}
