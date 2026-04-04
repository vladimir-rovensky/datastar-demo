package com.bookie.components;

import java.util.List;

import static com.bookie.infra.TemplatingEngine.format;

public class SelectInput extends BaseInput {
    private List<String> options;
    private String selected;

    public static SelectInput selectInput(String name, List<String> options, String selected) {
        var input = new SelectInput();
        input.name = name;
        input.options = options;
        input.selected = selected;
        return input;
    }

    //language=HTML
    @Override
    public String render() {
        var sb = new StringBuilder();
        for (String value : options) {
            if (value.equals(selected)) {
                sb.append("<option value=\"").append(value).append("\" selected>").append(value).append("</option>");
            } else {
                sb.append("<option value=\"").append(value).append("\">").append(value).append("</option>");
            }
        }
        return format("<select name=\"${name}\" data-signals='{${name}: null}' data-bind=\"${name}\" ${attrs}>${options}</select>",
                "name", name, "attrs", getAttrs(), "options", sb);
    }
}
