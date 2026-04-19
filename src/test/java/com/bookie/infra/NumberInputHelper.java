package com.bookie.infra;

import com.microsoft.playwright.Locator;

import java.math.BigDecimal;

public class NumberInputHelper extends FormFieldHelperBase {

    public NumberInputHelper(Locator root) {
        super(root);
    }

    public void setValue(BigDecimal value) {
        Locator input = root.locator("input");
        input.clear();
        input.fill(value.toPlainString());
    }
}
