package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import static com.bookie.infra.TemplatingEngine.html;

public class NumberInput extends BaseInput {
    private Number value;

    public static NumberInput numberInput(String name, Number value) {
        var input = new NumberInput();
        input.name = name;
        input.value = value;
        return input;
    }

    public NumberInput withValue(Number value) { this.value = value; return this; }

    @Override
    public EscapedHtml render() {
        return html("""
            <input type="number" name="${name}" data-signals='{${name}: "${value}"}' data-bind="${name}" value="${value}" @{attrs}>
        """,
                "name", this.name,
                "value", this.value != null ? this.value.toString() : "",
                "attrs", getAttrs());
    }
}
