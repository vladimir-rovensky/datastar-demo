package com.bookie.infra;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

public class LinkHelper {
    private final Locator root;

    public LinkHelper(Locator root) {
        this.root = root;
    }

    public static LinkHelper getByLabel(Locator root, String label) {
        return new LinkHelper(root.getByRole(AriaRole.LINK).filter(new Locator.FilterOptions().setHasText(label)).first());
    }

    public void click() {
        this.root.click();
    }
}
