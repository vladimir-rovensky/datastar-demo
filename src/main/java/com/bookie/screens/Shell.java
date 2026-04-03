package com.bookie.screens;

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
        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <title>%s</title>
                    <link rel="stylesheet" href="/global-styles.css">
                    <script type="module" src="/datastar.js"></script>
                </head>
                <body>
                %s
                </body>
                </html>
                """.formatted(title, content);
    }
}