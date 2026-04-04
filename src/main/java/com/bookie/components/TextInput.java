package com.bookie.components;

import static com.bookie.infra.TemplatingEngine.format;

public class TextInput {
    private String name;
    private String value;
    private String label;
    private String error;

    public static TextInput textInput(String name, String value) {
        return new TextInput()
                .withName(name)
                .withValue(value)
                .withLabel(startCase(name));
    }

    private static String startCase(String camelCase) {
        return Character.toUpperCase(camelCase.charAt(0)) + camelCase.substring(1).replaceAll("([A-Z])", " $1");
    }

    //language=HTML
    public String render() {

        return format("""
            <label>
                ${label}
                <div class="input-wrapper" ${error}>
                    <input type="text" name="${name}" data-bind="${name}" value="${value}" autocomplete='off' onfocus="this.select()">
                </div>
            </label>
        """,
                "label", this.label,
                "error", getErrorAttribute(),
                "name", this.name,
                "value", this.value != null ? this.value : "");
    }

    private String getErrorAttribute() {
        return this.error != null
                ? " data-error=\"" + this.error + "\""
                : "";
    }

    public TextInput withName(String name) {
        this.name = name;
        return this;
    }

    public TextInput withValue(String value) {
        this.value = value;
        return this;
    }

    public TextInput withLabel(String label) {
        this.label = label;
        return this;
    }

    public TextInput withError(String error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return render();
    }
}
