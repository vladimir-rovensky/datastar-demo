package com.bookie.components;

import com.bookie.infra.EscapedHtml;

import java.util.List;

import static com.bookie.infra.TemplatingEngine.html;

public class MultiselectInput extends BaseInput {
    private List<String> options;
    private List<String> value;

    public static MultiselectInput multiselectInput(String name, List<String> options, List<String> value) {
        var input = new MultiselectInput();
        input.name = name;
        input.options = options;
        input.value = value;
        return input;
    }

    @Override
    public EscapedHtml render() {
        var renderedOptions = EscapedHtml.concat(options, option -> html("""
                <option value="${v}">${v}</option>""", "v", option));

        return html("""
                <select multiple name="${name}" ${binding} ${attrs}>${options}</select>
                """,
                "name", name,
                "binding", getBindingAttr(value),
                "attrs", getAttrs(),
                "options", renderedOptions);
    }
}
