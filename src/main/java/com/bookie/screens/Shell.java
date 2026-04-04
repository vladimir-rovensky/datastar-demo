package com.bookie.screens;

import static com.bookie.infra.TemplatingEngine.format;

public class Shell {

    private String tabId = "";
    private String title = "";
    private String content = "";

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

    //language=HTML
    public String render() {
        return format("""
                <!DOCTYPE html>
                <html lang="en-US">
                <head>
                    <meta charset="UTF-8">
                    <title>Bookie - ${title}</title>
                    <link rel="stylesheet" href="/global-styles.css">
                    <script>
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
                <body data-init="@post('/updates', {retry: 'error'})">
                ${content}
                </body>
                </html>
                """,
                "title", title,
                "tabId", tabId,
                "content", content);
    }
}