package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Renderable;
import static com.bookie.infra.TemplatingEngine.html;

public class FormField implements Renderable {
    private String label;
    private String error;
    private EscapedHtml input;

    public static FormField formField(String label) {
        return new FormField().withLabel(label);
    }

    public EscapedHtml render() {
        return html("""
            <label>
                ${label}
                <div class="input-wrapper" @{error}>
                    @{input}
                </div>
            </label>
        """,
                "label", this.label,
                "error", this.error != null ? html("data-error=\"${msg}\"", "msg", this.error) : EscapedHtml.blank(),
                "input", this.input != null ? this.input : EscapedHtml.blank());
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
        this.input = input != null ? input.render() : null;
        return this;
    }

    @Override
    public String toString() {
        return render().rawHtml();
    }
}
