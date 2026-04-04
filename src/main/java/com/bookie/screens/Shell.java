package com.bookie.screens;

import java.util.UUID;

public class Shell {

    private String title = "";
    private String content = "";

    public static Shell shell() {
        return new Shell();
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
        var tabId = UUID.randomUUID().toString();
        return """
                <!DOCTYPE html>
                <html lang="en-US">
                <head>
                    <meta charset="UTF-8">
                    <title>Bookie - %s</title>
                    <link rel="stylesheet" href="/global-styles.css">
                    <script type="module" src="/datastar.js"></script>
                </head>
                <body data-signals="{tabId: '%s'}" data-init="@post('/updates', {retry: 'error'})">
                %s
                </body>
                </html>
                """.formatted(title, tabId, content);
    }
}