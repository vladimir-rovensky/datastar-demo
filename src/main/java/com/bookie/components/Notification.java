package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

import static com.bookie.infra.TemplatingEngine.html;

public class Notification implements Renderable {

    public enum NotificationStyle { WARNING, INFO, ERROR }

    public static final NotificationStyle warning = NotificationStyle.WARNING;
    public static final NotificationStyle info = NotificationStyle.INFO;
    public static final NotificationStyle error = NotificationStyle.ERROR;

    private final EscapedHtml content;
    private NotificationStyle style;
    private boolean inline = false;
    private String id = "global-notification";
    private EscapedHtml attributes = EscapedHtml.blank();

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

    public Notification inline() {
        this.inline = true;
        return this;
    }

    public Notification withID(String id) {
        this.id = id;
        return this;
    }

    public Notification withAttributes(EscapedHtml attributes) {
        this.attributes = attributes;
        return this;
    }

    @Override
    public EscapedHtml render() {
        var styleClass = style == null ? "" : switch (style) {
            case WARNING -> "notification--warning";
            case INFO    -> "notification--info";
            case ERROR   -> "notification--error";
        };

        var positionClass = inline ? "notification--inline" : "";

        return html("""
                <div id='${id}' role="alert" class="notification ${styleClass} ${positionClass}" data-on:click="el.hidden=true" ${attributes}>${content}</div>
                """,
                "id", this.id,
                "styleClass", styleClass,
                "positionClass", positionClass,
                "attributes", attributes,
                "content", content);
    }
}
