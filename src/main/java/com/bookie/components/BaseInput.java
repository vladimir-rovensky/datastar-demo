package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;
import com.bookie.infra.Util;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;

import static com.bookie.infra.TemplatingEngine.html;

public abstract class BaseInput implements Renderable {
    protected String name;
    protected String loadIndicator;
    protected boolean disabled;
    protected boolean bind = true;

    public abstract EscapedHtml render();

    protected EscapedHtml getAttrs() {
        if (loadIndicator != null) {
            return html("""
                    data-class="{loading: $${indicator}}" data-attr:disabled="$${indicator} || ${disabled}"\
                    """,
                    "indicator", loadIndicator,
                    "disabled", disabled);
        }

        return html(disabled ? "disabled" : "");
    }

    protected EscapedHtml getBindingAttr(Object value) {
        if(!this.bind) {
            return EscapedHtml.blank();
        }

        return html("""
                    data-signals:${name}="${value}" data-bind:${name}
                """,
                "name", Util.toKebabCase(this.name),
                "value", value != null
                        ? Util.toJson(value)
                        : "\"\"");
    }

    public BaseInput withDisabled(boolean disabled) { this.disabled = disabled; return this; }
    public BaseInput withLoadIndicator(String signal) { this.loadIndicator = signal; return this; }
    public BaseInput noBind() {  this.bind = false; return this; }
}
