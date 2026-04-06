package com.bookie.screens;

import com.bookie.infra.EscapedHtml;
import static com.bookie.infra.TemplatingEngine.html;

public class Shell {

    private String tabId = "";
    private String title = "";
    private EscapedHtml content = EscapedHtml.blank();
    private EscapedHtml toolbarContent = EscapedHtml.blank();

    public static Shell shell(String tabId) {
        var shell = new Shell();
        shell.tabId = tabId;
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

    public EscapedHtml render() {
        return html("""
                <!DOCTYPE html>
                <html lang="en-US">
                <head>
                    <meta charset="UTF-8">
                    <title>Bookie - ${title}</title>
                    <link rel="icon" type="image/svg+xml" href="/favicon.svg">
                    <link rel="stylesheet" href="/global-styles.css">
                    <script>
                    //We include the tabID with every request to make it easier on the backend. Would be nice if DataStar had a global way to do this.
                        const _fetch = window.fetch;
                        window.fetch = (url, opts = {}) => {
                            if (opts.headers?.['Datastar-Request']) {
                                opts.headers['X-tabID'] = '${tabId}';
                            }
                            return _fetch(url, opts);
                        };
                    </script>
                    <script type="module" src="/datastar.js"></script>
                </head>

                <body data-init="@get('/updates',  {openWhenHidden: true, retry: 'always'})" data-tab-id='${tabId}'>

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
                "nav", buildNav(),
                "content", content,
                "toolbarContent", toolbarContent);
    }

    private EscapedHtml buildNav() {
        var tradesLink = "Trades".equals(title)
                ? html("""
                        <a href="/trades" aria-current="page">Trades</a>
                        """)
                : html("""
                        <a href="/trades">Trades</a>
                        """);
        var positionsLink = "Positions".equals(title)
                ? html("""
                        <a href="/positions" aria-current="page">Positions</a>
                        """)
                : html("""
                        <a href="/positions">Positions</a>
                        """);
        return html("""
                <nav class="screen-nav">
                    ${tradesLink}
                    ${positionsLink}
                </nav>
                """, "tradesLink", tradesLink, "positionsLink", positionsLink);
    }
}