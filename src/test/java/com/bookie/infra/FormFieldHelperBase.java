package com.bookie.infra;

import com.microsoft.playwright.Locator;

public abstract class FormFieldHelperBase {
    protected final Locator root;

    protected FormFieldHelperBase(Locator root) {
        this.root = root;
    }
}
