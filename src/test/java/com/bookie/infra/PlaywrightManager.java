package com.bookie.infra;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

public class PlaywrightManager {

    private static final ThreadLocal<Playwright> playwrightPerThread = ThreadLocal.withInitial(() -> {
        Playwright playwright = Playwright.create();
        Runtime.getRuntime().addShutdownHook(new Thread(playwright::close));
        return playwright;
    });

    private static final ThreadLocal<Browser> browserPerThread = ThreadLocal.withInitial(() ->
        playwrightPerThread.get().chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))
    );

    public static Browser getBrowser() {
        return browserPerThread.get();
    }
}
