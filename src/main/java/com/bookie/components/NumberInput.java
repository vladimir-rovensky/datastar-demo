package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Util;

import static com.bookie.infra.TemplatingEngine.html;

public class NumberInput extends BaseInput {
    private Number value;
    private String format;
    private int decimals = 2;

    public static NumberInput numberInput(String name, Number value) {
        var input = new NumberInput();
        input.name = name;
        input.value = value;
        return input;
    }

    public NumberInput withFormat(String format) {
        this.format = format;
        return this;
    }

    public NumberInput withDecimals(int decimals) {
        this.decimals = decimals;
        return this;
    }

    @Override
    public EscapedHtml render() {
        return html("""
            <number-input name="${name}" format="${format}" decimals="${decimals}" ${binding} value="${value}" ${attrs}></number-input>
        """,
                "name", this.name,
                "format", this.format != null ? this.format : "",
                "decimals", this.decimals,
                "value", this.value != null ? this.value.toString() : "",
                "binding", this.getBindingAttr(value),
                "attrs", getAttrs());
    }
}
