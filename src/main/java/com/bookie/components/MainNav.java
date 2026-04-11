package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;
import com.bookie.infra.RouteInfo;

import static com.bookie.components.Link.link;
import static com.bookie.infra.TemplatingEngine.html;

public class MainNav implements Renderable {

    private RouteInfo routeInfo;
    private String activeTitle;

    public static MainNav mainNav() {
        return new MainNav();
    }

    public MainNav withRouteInfo(RouteInfo routeInfo) {
        this.routeInfo = routeInfo;
        return this;
    }

    public MainNav withActiveTitle(String title) {
        this.activeTitle = title;
        return this;
    }

    @Override
    public EscapedHtml render() {
        var tabId = routeInfo.tabId().localID();
        var securitiesPath = "securities/" + routeInfo.activeCusip() + "/" + routeInfo.activeSection();

        var tradesLink = link("trades", "Trades", tabId).withActive("Trades".equals(activeTitle));
        var positionsLink = link("positions", "Positions", tabId).withActive("Positions".equals(activeTitle));
        var securitiesLink = link(securitiesPath, "Securities", tabId).withActive("Securities".equals(activeTitle));

        return html("""
                <nav class="screen-nav">
                    ${tradesLink}
                    ${positionsLink}
                    ${securitiesLink}
                </nav>
                """,
                "tradesLink", tradesLink,
                "positionsLink", positionsLink,
                "securitiesLink", securitiesLink);
    }
}
