package com.bookie.infra;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class NotificationHelper {

    private final Locator root;

    public static NotificationHelper findError(Locator root) {
        return findNotification(root, ":scope.notification--error");
    }

    public static NotificationHelper findWarning(Locator root) {
        return findNotification(root, ":scope.notification--warning");
    }

    public static NotificationHelper findInfo(Locator root) {
        return findNotification(root, ":scope.notification--info");
    }

    private static NotificationHelper findNotification(Locator root, String selectorOrLocator) {
        return new NotificationHelper(root.getByRole(AriaRole.ALERT)
                .locator(selectorOrLocator));
    }

    public NotificationHelper(Locator root) {
        this.root = root;
    }

    public NotificationHelper verifyText(String error) {
        assertThat(this.root).containsText(error);
        return this;
    }
}
