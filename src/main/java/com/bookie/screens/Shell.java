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

                    <script type="module" src="/datastar1.0.0.js"></script>
                    <script type="module" src="/number-input.js"></script>
                </head>

                <body ${updateRequest}>

                    <div class="toolbar">
                        ${toolbarContent}
                        <div class="toolbar-separator"></div>
                        ${nav}
                        <span class="toolbar-title">${title}</span>
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
}
