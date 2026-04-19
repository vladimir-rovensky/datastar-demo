package com.bookie.infra;

import com.microsoft.playwright.Locator;

public class TextInputHelper extends FormFieldHelperBase {

    public TextInputHelper(Locator root) {
        super(root);
    }

    public void setValue(String value) {
        root.fill(value);
    }
}
