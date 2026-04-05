package com.bookie.screens;

import static com.bookie.infra.TemplatingEngine.format;

public class Shell {

    private String tabId = "";
    private String title = "";
    private String content = "";
    private String toolbarContent = "";

    public static Shell shell(String tabId) {
        var shell = new Shell();
        shell.tabId = tabId;
        return shell;
    }

    public Shell withTitle(String title) {
        this.title = title;
        return this;
    }

    public Shell withContent(String content) {
        this.content = content;
        return this;
    }

    public Shell withToolbar(String toolbarContent) {
        this.toolbarContent = toolbarContent;
        return this;
    }

    //language=HTML
    public String render() {
        return format("""
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
                <body data-init="@post('/updates', {retry: 'error'})" data-tab-id='${tabId}'>
                <div class="toolbar">
                    ${toolbarContent}
                    <span class="toolbar-title">${title}</span>
                </div>
                ${content}
                <div id="popup" data-signals__ifmissing="{popupVisible: false}"/>
                </body>
                </html>
                """,
                "title", title,
                "tabId", tabId,
                "content", content,
                "toolbarContent", toolbarContent);
    }
}