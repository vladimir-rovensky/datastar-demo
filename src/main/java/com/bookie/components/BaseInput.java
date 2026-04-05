package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

import static com.bookie.infra.TemplatingEngine.html;

public abstract class BaseInput implements Renderable {
    protected String name;
    protected String loadIndicator;
    protected boolean disabled;

    public abstract EscapedHtml render();

    protected EscapedHtml getAttrs() {
        if (loadIndicator != null) {
            return html("""
                    data-class="{loading: $" + loadIndicator + "}" data-attr:disabled="$" + loadIndicator + " || " + disabled + ""
                    """);
        }

        return html(disabled ? "disabled" : "");
    }

    public BaseInput withDisabled(boolean disabled) { this.disabled = disabled; return this; }
    public BaseInput withLoadIndicator(String signal) { this.loadIndicator = signal; return this; }

    @Override
    public String toString() { return render().html(); }
}
