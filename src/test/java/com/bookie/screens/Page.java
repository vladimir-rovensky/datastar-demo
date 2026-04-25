package com.bookie.screens;

import com.bookie.infra.HealthIndicatorHelper;
import com.bookie.infra.LinkHelper;
import com.bookie.infra.NotificationHelper;
import com.bookie.screens.positions.PositionsScreenPageObject;
import com.bookie.screens.securities.SecuritiesPageObject;
import com.bookie.screens.trades.TradesScreenPageObject;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class Page {

    private final String baseURL;
    private final com.microsoft.playwright.Page page;

    public Page(com.microsoft.playwright.Page page, String baseURL) {
        this.page = page;
        page.setDefaultTimeout(5000);

        this.baseURL = baseURL;
    }

    public void takeScreenshot(String name) {
        page.screenshot(new com.microsoft.playwright.Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/" + name + ".png"))
                .setFullPage(true));
    }

    public void reload() {
        page.reload();
    }

    public TradesScreenPageObject switchToTrades() {
        getSectionLink("Trades").click();
        waitForUpdatesToConnect();
        waitForMainSectionTitle("Trades");
        return new TradesScreenPageObject(page);
    }

    public PositionsScreenPageObject switchToPositions() {
        getSectionLink("Positions").click();
        waitForUpdatesToConnect();
        waitForMainSectionTitle("Positions");
        return new PositionsScreenPageObject(page);
    }

    public SecuritiesPageObject switchToSecurities(String cusip) {
        return switchToSecurities()
                .loadCusip(cusip);
    }

    public SecuritiesPageObject switchToSecurities() {
        getSectionLink("Securities").click();
        waitForUpdatesToConnect();
        waitForMainSectionTitle("Securities");
        return new SecuritiesPageObject(page, getHealthIndicator());
    }

    private void waitForMainSectionTitle(String title) {
        assertThat(page.getByRole(AriaRole.HEADING, new com.microsoft.playwright.Page.GetByRoleOptions().setLevel(1))).hasText(title);
    }

    public void waitForUpdatesToConnect() {
        getHealthIndicator().waitUntilHealthy();
    }

    private LinkHelper getSectionLink(String Trades) {
        return LinkHelper.getByLabel(getMainToolbar(), Trades);
    }

    private HealthIndicatorHelper getHealthIndicator() {
        return new HealthIndicatorHelper(getMainToolbar());
    }

    private Locator getMainToolbar() {
        return page.getByRole(AriaRole.TOOLBAR, new com.microsoft.playwright.Page.GetByRoleOptions().setName("Main Sections")).first();
    }

    public void assertWarningShown(String error) {
        NotificationHelper.findWarning(this.page.locator("body"))
                .verifyText(error);
    }

    public void assertErrorShown(String error) {
        NotificationHelper.findError(this.page.locator("body"))
                .verifyText(error);
    }

    public Page navigateToBookie() {
        page.navigate(baseURL);
        waitForUpdatesToConnect();
        return this;
    }

    public Page blockRoute(String pattern, Function<Integer, CompletableFuture<Void>> getUnblockSignal) {
        AtomicInteger callCount = new AtomicInteger(0);

        this.page.route(pattern, r -> {
            var callNumber = callCount.incrementAndGet();
            var signal = getUnblockSignal.apply(callNumber);
            signal.thenRun(r::resume);
        });

        return this;
    }

    public void verifyURL(String subpath) {
        this.page.waitForURL(subpath);
    }

    public void back() {
        this.page.goBack();
    }

    public void forward() {
        this.page.goForward();
    }
}
