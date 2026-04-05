package com.bookie.components;

import java.util.List;

import com.bookie.infra.EscapedHtml;
import static com.bookie.infra.TemplatingEngine.html;

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

    @Override
    public EscapedHtml render() {
        var renderedOptions = EscapedHtml.concat(options, value -> html(
                value.equals(selected)
                        ? "<option value=\"${v}\" selected>${v}</option>"
                        : "<option value=\"${v}\">${v}</option>",
                "v", value));

        return html("""
                        <select name="${name}" data-signals='{${name}: "${selected}"}' data-bind="${name}" @{attrs}>@{options}</select>
            """,
                "name", name,
                "selected", selected != null ? selected : "",
                "attrs", getAttrs(),
                "options", renderedOptions);
    }
}
