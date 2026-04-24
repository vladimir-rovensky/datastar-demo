package com.bookie.screens;

import com.bookie.components.Notification;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.FetchBuilder.Retry;
import com.bookie.infra.RouteInfo;

import static com.bookie.components.MainNav.mainNav;
import static com.bookie.infra.HtmlExtensions.X;
import static com.bookie.infra.TemplatingEngine.html;

public class Shell {

    private RouteInfo routeInfo;
    private String title = "";
    private String updateURL;
    private boolean openWhenHidden;
    private boolean orderRequests = true;
    private EscapedHtml content = EscapedHtml.blank();
    private EscapedHtml toolbarContent = EscapedHtml.blank();

    public static Shell shell(RouteInfo routeInfo) {
        var shell = new Shell();
        shell.routeInfo = routeInfo;
        return shell;
    }

    public Shell withTitle(String title) {
        this.title = title;
        return this;
    }

    public Shell withContent(EscapedHtml content) {
        this.content = content;
        return this;
    }

    public Shell withToolbar(EscapedHtml toolbarContent) {
        this.toolbarContent = toolbarContent;
        return this;
    }

    public Shell withUpdateURL(String updateURL) {
        this.updateURL = updateURL;
        return this;
    }

    public Shell withOpenWhenHidden(boolean openWhenHidden) {
        this.openWhenHidden = openWhenHidden;
        return this;
    }

    public Shell withOrderRequests(boolean orderRequests) {
        this.orderRequests = orderRequests;
        return this;
    }

    public EscapedHtml render() {
        var tabId = routeInfo.tabId().localID();
        var nav = mainNav()
                .withRouteInfo(routeInfo)
                .withActiveTitle(title);

        return html("""
                <!DOCTYPE html>
                <html lang="en-US">
                <head>
                    <meta charset="UTF-8">
                    <title>Bookie - ${title}</title>
                    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
                    <link rel="stylesheet" href="/global-styles.css">

                    <script>window.__tabID = '${tabId}'; window.__orderRequests = ${orderRequests}</script>
                    <script src="/fetch-monkeypatch.js"></script>

                    <script type="module" src="/datastar1.0.0.RC8.js"></script>
                    <script type="module" src="/number-input.js"></script>
                </head>

                <body   data-signals:_update-status="'pending'"
                        data-signals:_failed-request="false"
                        data-on:popstate__window="${popstateAction}"
                        data-on:datastar-fetch="
                            if(evt.detail.el === document.body) {
                                switch(evt.detail.type) {
                                    case 'started': $_updateStatus = 'ok'; break;
                                    case 'error': $_updateStatus = 'error'; break;
                                    case 'retrying': $_updateStatus = 'warn'; break;
                                    case 'retries-failed': $_updateStatus = 'error'; break;
                                }
                            }
                            if(evt.detail.el !== document.body && (evt.detail.type === 'error' || evt.detail.type === 'retries-failed')) {
                                $_failedRequest = true;
                            }"
                        ${updateRequest}>

                    <div class="toolbar" role="toolbar" aria-label="Main Sections">
                        ${toolbarContent}
                        <div class="toolbar-separator"></div>
                        ${nav}
                        <span class="toolbar-title" role="heading" aria-level="1">${title}</span>
                        <div class="toolbar-separator"></div>
                        ${healthIndicator}
                    </div>

                    ${content}

                <div id="popup" data-signals__ifmissing="{popupVisible: false}"></div>
                
                <div id="global-notification"></div>
                ${failedRequestNotification}

                <script id="script-runner">
                </script>

                </body>
                </html>
                """,
                "title", title,
                "tabId", tabId,
                "orderRequests", orderRequests,
                "popstateAction", X.get(html("window.location.pathname + window.location.search")),
                "healthIndicator", getHealthIndicator(),
                "updateRequest", getUpdateRequestAttribute(),
                "nav", nav,
                "content", content,
                "toolbarContent", toolbarContent,
                "failedRequestNotification", getFailedRequestNotification());
    }

    private static Notification getFailedRequestNotification() {
        return Notification.notification(html("Lost connection to the server. Please refresh the page."))
                .withStyle(Notification.error)
                .withID("notification-connection-error")
                .hideOnClick(false)
                .withAttributes(html("style='display:none' data-show='!!$_failedRequest' data-on:click='$_failedRequest=false'"));
    }

    private EscapedHtml getUpdateRequestAttribute() {
        if (this.updateURL == null) {
            return EscapedHtml.blank();
        }

        return html("""
                data-init="${action}"
        """, "action", X.get(this.updateURL)
                .withOpenWhenHidden(this.openWhenHidden)
                .withRetry(Retry.ALWAYS)
                .withRequestCancellation(true));
    }

    private EscapedHtml getHealthIndicator() {
        return html("""
            <span class="health-indicator" role="meter" aria-label="Connection Health"
                  data-tooltip="Connecting"
                  data-attr:data-tooltip="$_updateStatus === 'ok' ? 'Connected' : $_updateStatus === 'warn' ? 'Reconnecting' : $_updateStatus === 'error' ? 'Connection Error' : 'Connecting'"
                  data-class="{ok: $_updateStatus === 'ok', warn: $_updateStatus === 'warn', error: $_updateStatus === 'error'}">
            </span>""");
    }
}
