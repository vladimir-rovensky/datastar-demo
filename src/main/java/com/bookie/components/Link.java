package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

import static com.bookie.infra.HtmlExtensions.X;
import static com.bookie.infra.TemplatingEngine.html;

public class Link implements Renderable {

    private final String url;
    private final String label;
    private boolean active = false;

    private Link(String url, String label) {
        this.url = url;
        this.label = label;
    }

    public static Link link(String url, String label) {
        return new Link(url, label);
    }

    public Link withActive(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public EscapedHtml render() {
        var href = "/" + url;
        var ariaCurrent = active ? html(" aria-current=\"page\"") : EscapedHtml.blank();
        return html("""
                <a href="${href}" data-on:click="${clickHandler}" ${ariaCurrent}>${label}</a>
                """,
                "href", href,
                "clickHandler", getClickHandler(href),
                "ariaCurrent", ariaCurrent,
                "label", label);
    }

    private EscapedHtml getClickHandler(String href) {
        return html("""
                if(!evt.ctrlKey && !evt.metaKey && !evt.shiftKey && !evt.altKey && evt.button===0) { evt.preventDefault(); history.pushState(null,'','${href}'); ${getAction}; }
                """,
                "href", href,
                "getAction", X.get(href).withExcludeAllSignals());
    }

}
