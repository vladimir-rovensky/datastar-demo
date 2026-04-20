package com.bookie.infra;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class HealthIndicatorHelper {
    private final Locator root;

    public HealthIndicatorHelper(Locator root) {
        this.root = root.getByRole(AriaRole.METER);;
    }

    public void waitUntilHealthy() {
        assertThat(root).hasClass("health-indicator ok");
    }
}
