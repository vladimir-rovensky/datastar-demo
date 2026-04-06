package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

import static com.bookie.infra.TemplatingEngine.html;

public class Link implements Renderable {

    private final String screen;
    private final String tabId;
    private boolean active = false;

    private Link(String screen, String tabId) {
        this.screen = screen;
        this.tabId = tabId;
    }

    public static Link link(String screen, String tabId) {
        return new Link(screen, tabId);
    }

    public Link withActive(boolean active) {
        this.active = active;
        return this;
    }

    @Override
    public EscapedHtml render() {
        var label = Character.toUpperCase(screen.charAt(0)) + screen.substring(1);
        if (active) {
            return html("""
                    <a href="/${screen}?tabID=${tabId}" aria-current="page">${label}</a>
                    """, "screen", screen, "tabId", tabId, "label", label);
        }
        return html("""
                <a href="/${screen}?tabID=${tabId}">${label}</a>
                """, "screen", screen, "tabId", tabId, "label", label);
    }
}
