package com.bookie.infra;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class ButtonHelper {
    private final Locator root;

    public ButtonHelper(Locator root) {
        this.root = root;
    }

    public static ButtonHelper getByLabel(Locator root, String label) {
        return new ButtonHelper(root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(label))
                .first());
    }

    public static ButtonHelper getByAnyLabel(Locator root, List<String> candidateLabels) {
        return new ButtonHelper(root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions()
                .setName(Pattern.compile(String.join("|", candidateLabels))))
                .first());
    }

    public void click() {
        this.root.click();
    }

    public boolean isClickable() {
        return this.root.isEnabled() && this.root.isVisible();
    }
}
