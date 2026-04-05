package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.Map;

import static com.bookie.infra.Response.patchSignals;
import static com.bookie.infra.Response.sse;
import static com.bookie.infra.TemplatingEngine.html;

public class Popup {

    private String title;
    private EscapedHtml content;
    private EscapedHtml actions;

    public static Popup popup() {
        return new Popup();
    }

    public Popup withTitle(String title) {
        this.title = title;
        return this;
    }

    public Popup withContent(EscapedHtml content) {
        this.content = content != null ? content.render() : EscapedHtml.blank();
        return this;
    }

    public Popup withActions(EscapedHtml actions) {
        this.actions = actions != null ? actions.render() : EscapedHtml.blank();
        return this;
    }

    public EscapedHtml render() {
        return html("""
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
                        @{content}
                        <div class="popup-actions">
                            @{actions}
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
        return render().rawHtml();
    }

    public static ServerResponse open(EscapedHtml content) {
        return sse(ch -> ch
                .updateFragment(content)
                .patchSignals(Map.of("popupVisible", true))
                .complete());
    }

    public static ServerResponse close() {
        return patchSignals(Map.of("popupVisible", false));
    }
}
