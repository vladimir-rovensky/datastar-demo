package com.bookie.components;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Util;

import static com.bookie.infra.TemplatingEngine.html;

public class TextInput extends BaseInput {
    private String value;

    public static TextInput textInput(String name, String value) {
        var input = new TextInput();
        input.name = name;
        input.value = value;
        return input;
    }

    @Override
    public EscapedHtml render() {
        return html("""
            <input type="text" name="${name}" data-signals='{nonce: "${nonce}", ${name}: "${value}"}' data-bind="${name}" value="${value}" autocomplete="off" ${attrs}>
        """,
                "name", this.name,
                "value", this.value != null ? this.value : "",
                "nonce", Util.nonce(),
                "attrs", getAttrs());
    }
}
