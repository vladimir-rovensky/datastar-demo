package com.bookie.infra;

import com.microsoft.playwright.Locator;

import java.math.BigDecimal;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class NumberInputHelper extends FormFieldHelperBase {

    public NumberInputHelper(Locator root) {
        super(root);
    }

    public void setValue(Number value) {
        Locator input = root.locator("input");
        input.clear();
        input.fill(new BigDecimal(value.toString()).toPlainString());
        input.blur();
    }

    public void verifyValue(Number expected) {
        Locator input = root.locator("number-input");
        assertThat(input).hasAttribute("value", new BigDecimal(expected.toString()).toPlainString());
    }
}
