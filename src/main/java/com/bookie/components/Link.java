package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;
import org.jetbrains.annotations.NotNull;

import static com.bookie.infra.TemplatingEngine.html;

public class Link implements Renderable {

    private final String url;
    private final String label;
    private final String tabId;
    private boolean active = false;

    private Link(String url, String label, String tabId) {
        this.url = url;
        this.label = label;
        this.tabId = tabId;
    }

    public static Link link(String url, String label, String tabId) {
        return new Link(url, label, tabId);
    }

    public Link withActive(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public EscapedHtml render() {
        var href = getHref();
        var ariaCurrent = active ? html(" aria-current=\"page\"") : EscapedHtml.blank();
        return html("""
                <a href="${href}"${ariaCurrent}>${label}</a>
                """,
                "href", href,
                "ariaCurrent", ariaCurrent,
                "label", label);
    }

    public @NotNull String getHref() {
        return "/" + url + "?tabID=" + tabId;
    }
}
