package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;
import static com.bookie.infra.TemplatingEngine.html;

public class FormField implements Renderable {
    private String label;
    private String error;
    private BaseInput input;

    public static FormField formField(String label) {
        return new FormField().withLabel(label);
    }

    public static FormField formField() {
        return new FormField().withLabel("");
    }

    public EscapedHtml render() {
        var showError = this.input != null && !this.input.isDisabled();
        var errorAttr = showError && this.error != null ? html("data-error=\"${msg}\"", "msg", this.error) : EscapedHtml.blank();
        var inputContent = this.input != null ? this.input.render() : EscapedHtml.blank();

        if (this.label == null || this.label.isBlank()) {
            return html("""
                <div class="input-wrapper" ${error}>
                    ${input}
                </div>
            """,
                    "error", errorAttr,
                    "input", inputContent);
        }

        return html("""
            <label>
                ${label}
                <div class="input-wrapper" ${error}>
                    ${input}
                </div>
            </label>
        """,
                "label", this.label,
                "error", errorAttr,
                "input", inputContent);
    }

    public FormField withLabel(String label) {
        this.label = label;
        return this;
    }

    public FormField withError(String error) {
        this.error = error;
        return this;
    }

    public FormField withInput(BaseInput input) {
        this.input = input;
        return this;
    }

}
