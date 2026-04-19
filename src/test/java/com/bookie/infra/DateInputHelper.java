package com.bookie.infra;

import com.microsoft.playwright.Locator;

import java.time.LocalDate;

public class DateInputHelper extends FormFieldHelperBase {

    public DateInputHelper(Locator root) {
        super(root);
    }

    public void setValue(LocalDate value) {
        root.fill(value.toString());
    }
}
