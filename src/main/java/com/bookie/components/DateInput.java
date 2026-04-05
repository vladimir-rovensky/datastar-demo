package com.bookie.components;

import java.time.LocalDate;

import static com.bookie.infra.TemplatingEngine.format;

public class DateInput extends BaseInput {
    private LocalDate value;

    public static DateInput dateInput(String name, LocalDate value) {
        var input = new DateInput();
        input.name = name;
        input.value = value;
        return input;
    }

    //language=HTML
    @Override
    public String render() {
        return format("""
            <input type="date" name="${name}" data-signals='{${name}: "${value}"}' data-bind="${name}" value="${value}" ${attrs}>
        """,
                "name", this.name,
                "value", this.value != null ? this.value.toString() : "",
                "attrs", getAttrs());
    }
}
