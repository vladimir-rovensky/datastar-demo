package com.bookie.screens;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.RouteInfo;
import com.bookie.infra.Util;

import java.util.List;

import static com.bookie.components.MainNav.mainNav;
import static com.bookie.infra.TemplatingEngine.html;

public class Shell {

    private RouteInfo routeInfo;
    private String title = "";
    private String updateURL;
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

    public EscapedHtml render() {
        var tabId = routeInfo.tabId().localID();
        var nav = mainNav()
                .withRouteInfo(routeInfo)
                .withActiveTitle(title);

        var prerenderURLs = html(Util.toJson(List.of(nav.getTradesLink().getHref(), nav.getPositionsLink().getHref(), nav.getSecuritiesLink().getHref())));

        return html("""
                <!DOCTYPE html>
                <html lang="en-US">
                <head>
                    <meta charset="UTF-8">
                    <title>Bookie - ${title}</title>
                    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
                    <link rel="stylesheet" href="/global-styles.css">

                    <script type="speculationrules" data-ignore-morph>
                      {
                        "prerender": [{
                          "urls": ${prerenderURLs}
                        }]
                      }
                    </script>

                    <script>
                        //We include the tabID with every request to make it easier on the backend. Would be nice if DataStar had a global way to do this.        \s
                        const _fetch = window.fetch;
                        window.fetch = (url, opts = {}) => {
                            if (opts.headers?.['Datastar-Request']) {
                                opts.headers['X-tabID'] = '${tabId}';
                            }
                            return _fetch(url, opts);
                        };
                        const _tabUrl = new URL(window.location);
                        _tabUrl.searchParams.set('tabID', '${tabId}');
                        history.replaceState(null, '', _tabUrl);
                    </script>

                    <script type="module" src="/datastar1.0.0.RC8.js"></script>
                    <script type="module" src="/number-input.js"></script>
                </head>

                <body   data-signals:_update-status="'pending'"
                        data-on:datastar-fetch="
                            if(evt.detail.el === document.body) {
                                switch(evt.detail.type) {
                                    case 'started': $_updateStatus = 'ok'; break;
                                    case 'error': $_updateStatus = 'error'; break;
                                    case 'retrying': $_updateStatus = 'warn'; break;
                                    case 'retries-failed': $_updateStatus = 'error'; break;
                                }
                            }"
                        ${updateRequest}>

                    <div class="toolbar" role="toolbar" aria-label="Main Sections">
                        ${toolbarContent}
                        <div class="toolbar-separator"></div>
                        ${nav}
                        <span class="toolbar-title">${title}</span>
                        <div class="toolbar-separator"></div>
                        ${healthIndicator}
                    </div>

                    ${content}

                <div id="popup" data-signals__ifmissing="{popupVisible: false}">
                </div>

                <script id="script-runner">
                </script>

                </body>
                </html>
                """,
                "title", title,
                "tabId", tabId,
                "healthIndicator", getHealthIndicator(),
                "prerenderURLs", prerenderURLs,
                "updateRequest", getUpdateRequestAttribute(),
                "nav", nav,
                "content", content,
                "toolbarContent", toolbarContent);
    }

    private EscapedHtml getUpdateRequestAttribute() {
        if (this.updateURL == null) {
            return EscapedHtml.blank();
        }

        return html("""
                data-init="@post('${url}', {openWhenHidden: true, retry: 'always'})"
        """, "url", this.updateURL);
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
