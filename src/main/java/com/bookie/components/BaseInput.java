package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;

public abstract class BaseInput implements Renderable {
    protected String name;
    protected String loadIndicator;
    protected boolean disabled;

    public abstract EscapedHtml render();

    protected EscapedHtml getAttrs() {
        if (loadIndicator != null) {
            return EscapedHtml.html("data-class=\"{loading: $" + loadIndicator + "}\" data-attr:disabled=\"$" + loadIndicator + " || " + disabled + "\"");
        }

        return EscapedHtml.html(disabled ? "disabled" : "");
    }

    public BaseInput withDisabled(boolean disabled) { this.disabled = disabled; return this; }
    public BaseInput withLoadIndicator(String signal) { this.loadIndicator = signal; return this; }

    @Override
    public String toString() { return render().html(); }
}
