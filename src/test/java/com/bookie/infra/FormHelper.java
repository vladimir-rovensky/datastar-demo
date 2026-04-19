package com.bookie.infra;

import com.microsoft.playwright.Locator;

public class FormHelper {
    private final Locator root;

    public FormHelper(Locator root) {
        this.root = root;
    }

    public TextInputHelper getTextField(String label) {
        return new TextInputHelper(root.getByLabel(label));
    }

    public NumberInputHelper getNumericField(String label) {
        return new NumberInputHelper(root.getByText(label));
    }

    public SelectInputHelper getSelectField(String label) {
        return new SelectInputHelper(root.getByLabel(label));
    }

    public DateInputHelper getDateField(String label) {
        return new DateInputHelper(root.getByLabel(label));
    }
}
