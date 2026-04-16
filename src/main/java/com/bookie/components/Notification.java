package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

import static com.bookie.infra.TemplatingEngine.html;

public class Notification implements Renderable {

    public enum NotificationStyle { WARNING }

    public static final NotificationStyle warning = NotificationStyle.WARNING;

    private final EscapedHtml content;
    private NotificationStyle style;

    private Notification(EscapedHtml content) {
        this.content = content;
    }

    public static Notification notification(EscapedHtml content) {
        return new Notification(content);
    }

    public Notification withStyle(NotificationStyle style) {
        this.style = style;
        return this;
    }

    @Override
    public EscapedHtml render() {
        var styleClass = style == NotificationStyle.WARNING ? "notification--warning" : "";
        return html("""
                <div class="notification ${styleClass}">${content}</div>
                """,
                "styleClass", styleClass,
                "content", content);
    }
}
