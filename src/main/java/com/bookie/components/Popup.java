package com.bookie.components;

import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

import static com.bookie.infra.Response.patchSignals;
import static com.bookie.infra.Response.sse;
import static com.bookie.infra.TemplatingEngine.format;

public class Popup {

    private String title;
    private String content;
    private String actions;

    public static Popup popup() {
        return new Popup();
    }

    public Popup withTitle(String title) {
        this.title = title;
        return this;
    }

    public Popup withContent(Object content) {
        this.content = content != null ? content.toString() : "";
        return this;
    }

    public Popup withActions(Object actions) {
        this.actions = actions != null ? actions.toString() : "";
        return this;
    }

    //language=HTML
    public String render() {
        return format("""
                <div id="popup" class="popup-overlay" data-show="$popupVisible">
                    <div class="popup"
                         data-signals:_dx__ifmissing="0"
                         data-signals:_dy__ifmissing="0"
                         data-signals:_dragging__ifmissing="false"
                         data-style="{'transform': 'translate(' + $_dx + 'px,' + $_dy + 'px)'}">
                        <div class="popup-title"
                             data-on:pointerdown__prevent="$_dragging = true; el.setPointerCapture(evt.pointerId)"
                             data-on:pointermove="if ($_dragging) { $_dx += evt.movementX; $_dy += evt.movementY }"
                             data-on:pointerup="$_dragging = false">
                            ${title}
                        </div>
                        ${content}
                        <div class="popup-actions">
                            ${actions}
                        </div>
                    </div>
                </div>
                """,
                "title", title,
                "content", content,
                "actions", actions);
    }

    @Override
    public String toString() {
        return render();
    }

    public static ServerResponse open(String content) {
        return sse(ch -> ch
                .updateFragment(content)
                .patchSignals(Map.of("popupVisible", true))
                .complete());
    }

    public static ServerResponse close() {
        return patchSignals(Map.of("popupVisible", false));
    }
}
