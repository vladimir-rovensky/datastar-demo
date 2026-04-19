package com.bookie.infra;

import com.microsoft.playwright.Locator;

public class SelectInputHelper extends FormFieldHelperBase {

    public SelectInputHelper(Locator root) {
        super(root);
    }

    public void setValue(String value) {
        root.selectOption(value);
    }
}
