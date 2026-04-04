package com.bookie.components;

import static com.bookie.infra.TemplatingEngine.format;

public class FormField {
    private String label;
    private String error;
    private String input;

    public static FormField formField(String label) {
        return new FormField().withLabel(label);
    }

    //language=HTML
    public String render() {
        return format("""
            <label>
                ${label}
                <div class="input-wrapper" ${error}>
                    ${input}
                </div>
            </label>
        """,
                "label", this.label,
                "error", this.error != null ? "data-error=\"" + this.error + "\"" : "",
                "input", this.input != null ? this.input : "");
    }

    public FormField withLabel(String label) {
        this.label = label;
        return this;
    }

    public FormField withError(String error) {
        this.error = error;
        return this;
    }

    public FormField withInput(Object input) {
        this.input = input != null ? input.toString() : null;
        return this;
    }

    @Override
    public String toString() {
        return render();
    }
}
