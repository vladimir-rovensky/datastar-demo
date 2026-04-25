package com.bookie.infra;

import com.microsoft.playwright.Locator;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class TextInputHelper extends FormFieldHelperBase {

    public TextInputHelper(Locator root) {
        super(root);
    }

    public TextInputHelper setValue(String value) {
        root.fill(value);
        return this;
    }

    public TextInputHelper verifyValue(String value) {
        assertThat(root).hasValue(value);
        return this;
    }

    public TextInputHelper verifyEnablement(boolean enabled) {
        if(enabled) {
            assertThat(root).not().isDisabled();
        } else {
            assertThat(root).isDisabled();
        }

        return this;
    }
}
