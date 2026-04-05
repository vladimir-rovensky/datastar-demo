package com.bookie.components;

import java.time.LocalDate;

import com.bookie.infra.EscapedHtml;
import static com.bookie.infra.TemplatingEngine.html;

public class DateInput extends BaseInput {
    private LocalDate value;

    public static DateInput dateInput(String name, LocalDate value) {
        var input = new DateInput();
        input.name = name;
        input.value = value;
        return input;
    }

    @Override
    public EscapedHtml render() {
        return html("""
            <input type="date" name="${name}" data-signals='{${name}: "${value}"}' data-bind="${name}" value="${value}" @{attrs}>
        """,
                "name", this.name,
                "value", this.value != null ? this.value.toString() : "",
                "attrs", getAttrs());
    }
}
