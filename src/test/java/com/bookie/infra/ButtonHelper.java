package com.bookie.infra;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class ButtonHelper {
    private final Locator root;

    public ButtonHelper(Locator root) {
        this.root = root;
    }

    public static ButtonHelper getByLabel(Locator root, String label) {
        return new ButtonHelper(root.getByRole(AriaRole.BUTTON)
                .filter(new Locator.FilterOptions().setHasText(label))
                .first());
    }

    public void click() {
        this.root.click();
    }
}
