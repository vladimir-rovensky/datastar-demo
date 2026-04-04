package com.bookie.components;

public abstract class BaseInput {
    protected String name;
    protected String loadIndicator;
    protected boolean disabled;

    public abstract String render();

    protected String getAttrs() {
        if (loadIndicator != null) {
            return "data-class=\"{loading: $" + loadIndicator + "}\" data-attr:disabled=\"$" + loadIndicator + " || " + disabled + "\"";
        }

        return disabled ? "disabled" : "";
    }

    public BaseInput withName(String name) { this.name = name; return this; }
    public BaseInput withDisabled(boolean disabled) { this.disabled = disabled; return this; }
    public BaseInput withLoadIndicator(String signal) { this.loadIndicator = signal; return this; }

    @Override
    public String toString() { return render(); }
}
