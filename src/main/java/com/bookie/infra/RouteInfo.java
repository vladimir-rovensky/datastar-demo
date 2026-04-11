package com.bookie.infra;

public record RouteInfo(TabID tabId, String activeCusip, String activeSection) {

    public static RouteInfo initial(TabID tabId) {
        return new RouteInfo(tabId, "nocusip", "general");
    }

    public RouteInfo withActiveCusip(String cusip) {
        return new RouteInfo(tabId, cusip, activeSection);
    }

    public RouteInfo withActiveSection(String section) {
        return new RouteInfo(tabId, activeCusip, section);
    }
}
