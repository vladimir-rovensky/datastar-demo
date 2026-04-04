package com.bookie.components;

import static com.bookie.infra.TemplatingEngine.format;

public class TextInput extends BaseInput {
    private String value;

    public static TextInput textInput(String name, String value) {
        var input = new TextInput();
        input.name = name;
        input.value = value;
        return input;
    }

    public TextInput withValue(String value) { this.value = value; return this; }

    //language=HTML
    @Override
    public String render() {
        return format("""
            <input type="text" name="${name}" data-bind="${name}" value="${value}" autocomplete="off" onfocus="this.select()" ${attrs}>
        """,
                "name", this.name,
                "value", this.value != null ? this.value : "",
                "attrs", getAttrs());
    }
}
