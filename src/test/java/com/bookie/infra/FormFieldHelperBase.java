package com.bookie.infra;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public abstract class FormFieldHelperBase {
    protected final Locator root;

    protected FormFieldHelperBase(Locator root) {
        this.root = root;
    }

    public void verifyError(String error) {
        assertThat(this.root.locator("[data-error]")).hasAttribute("data-error", error, new LocatorAssertions.HasAttributeOptions().setIgnoreCase(true));
    }
}
