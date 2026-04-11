package com.bookie.components;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.bookie.infra.EscapedHtml;
import com.bookie.infra.Util;

import static com.bookie.infra.TemplatingEngine.html;

public class SelectInput extends BaseInput {
    private List<String> options;
    private Map<String, String> labels;
    private String selected;

    public static <T extends Enum<T>> SelectInput selectInput(String name, Class<T> enumClass, T selected) {
        var options = Arrays.stream(enumClass.getEnumConstants()).map(Enum::name).toList();
        return selectInput(name, options, selected != null ? selected.name() : null);
    }

    public static SelectInput selectInput(String name, Class<Boolean> ignored, boolean selected) {
        var input = new SelectInput();
        input.name = name;
        input.options = List.of("true", "false");
        input.labels = Map.of("true", "Yes", "false", "No");
        input.selected = String.valueOf(selected);
        return input;
    }

    public static SelectInput selectInput(String name, List<String> options, String selected) {
        var input = new SelectInput();
        input.name = name;
        input.options = options;
        input.selected = selected;
        return input;
    }

    @Override
    public EscapedHtml render() {
        var renderedOptions = EscapedHtml.concat(options, value -> {
            var label = labels != null ? labels.getOrDefault(value, value) : value;
            return html(
                    value.equals(selected)
                            ? "<option value=\"${v}\" selected>${label}</option>"
                            : "<option value=\"${v}\">${label}</option>",
                    "v", value, "label", label);
        });

        return html("""
                        <select name="${name}" ${binding} ${attrs}>${options}</select>
            """,
                "name", name,
                "selected", selected != null ? selected : "",
                "attrs", getAttrs(),
                "options", renderedOptions);
    }
}
