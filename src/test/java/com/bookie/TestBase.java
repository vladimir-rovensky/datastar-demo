package com.bookie;

import com.bookie.domain.entity.*;
import com.bookie.domain.service.PositionService;
import com.bookie.infra.*;
import com.bookie.screens.positions.PositionsScreenPageObject;
import com.bookie.screens.securities.SecuritiesPageObject;
import com.bookie.screens.trades.TradesScreenPageObject;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.*;
import org.springframework.lang.NonNull;

import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase {

    private JettyBootstrap server;
    private BrowserContext browserContext;
    private Page page;
    private final boolean isolateTests = false; //False -> faster, but tests use the same tab/context

    @BeforeAll
    void startServer() throws Exception {
        server = JettyBootstrap.start();

        if(!isolateTests) {
            prepareTab();
        }
    }

    @AfterAll
    void stopServer() throws Exception {
        if(!isolateTests) {
            cleanupTab();
        }

        server.stop();
    }

    @BeforeEach
    void setup() {
        getSessionRegistry().clearAll();
        getBondDAO().clear();
        getTradeDAO().clear();
        getPositionService().clear();

        if(isolateTests) {
            prepareTab();
        }

        navigateToBookie();
    }

    @AfterEach
    void closeBrowserContext(TestInfo ignored) {
        //takeScreenshot(ignored.getDisplayName());

        if(isolateTests) {
            cleanupTab();
        }
    }

    private void prepareTab() {
        browserContext = PlaywrightManager.getBrowser().newContext();
        page = browserContext.newPage();
        page.setDefaultTimeout(5000);
    }

    private void cleanupTab() {
        browserContext.close();
    }

    protected void takeScreenshot(String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get("screenshots/" + name + ".png"))
                .setFullPage(true));
    }

    protected void reloadPage() {
        this.page.reload();
    }

    protected HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    protected void trace(Runnable test) {
        try {
            startTracing();
            navigateToBookie();
            test.run();
        } finally {
            stopTracing();
        }
    }

    protected void startTracing() {
        this.browserContext.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
    }

    protected void stopTracing() {
        this.browserContext.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get("trace.zip")));
    }

    protected FakeTradeDAO getTradeDAO() {
        return server.getBean(FakeTradeDAO.class);
    }

    protected PositionService getPositionService() {
        return server.getBean(PositionService.class);
    }

    protected BondRepository getBondRepository() {
        return server.getBean(BondRepository.class);
    }

    protected FakeBondDAO getBondDAO() {
        return server.getBean(FakeBondDAO.class);
    }

    protected TradeRepository getTradeRepository() {
        return server.getBean(TradeRepository.class);
    }

    private SessionRegistry getSessionRegistry() {
        return server.getBean(SessionRegistry.class);
    }

    protected String baseUrl() {
        return "http://localhost:" + server.getPort();
    }

    protected void bookTrades(Trade... trades) {
        givenExistingTrades(trades);
    }

    protected void givenExistingTrades(Trade... trades) {
        givenExistingTrades(List.of(trades));
    }

    protected void givenExistingTrades(List<Trade> trades) {
        trades.forEach(t -> {
            if(!getTradeRepository().bookTrade(t)) {
                throw new RuntimeException("Failed to book an invalid trade: " + t);
            }
        });
    }

    protected Bond getSavedBond(String cusip) {
        return getBondDAO().findByCusip(cusip);
    }

    protected void saveBonds(Bond... bonds) {
        givenExistingBonds(bonds);
    }

    protected void givenExistingBonds(Bond... bonds) {
        givenExistingBonds(List.of(bonds));
    }

    protected void givenExistingBonds(List<Bond> bonds) {
        bonds.forEach(b -> getBondRepository().saveBond(b));
    }

    protected TradesScreenPageObject switchToTrades() {
        getSectionLink("Trades").click();
        waitForUpdatesToConnect();
        waitForMainSectionTitle("Trades");
        return new TradesScreenPageObject(page);
    }

    protected PositionsScreenPageObject switchToPositions() {
        getSectionLink("Positions").click();
        waitForUpdatesToConnect();
        waitForMainSectionTitle("Positions");
        return new PositionsScreenPageObject(page);
    }

    protected SecuritiesPageObject switchToSecurities(String cusip) {
        return switchToSecurities()
                .loadCusip(cusip);
    }

    protected SecuritiesPageObject switchToSecurities() {
        getSectionLink("Securities").click();
        waitForUpdatesToConnect();
        waitForMainSectionTitle("Securities");
        return new SecuritiesPageObject(page, getHealthIndicator());
    }

    private void waitForMainSectionTitle(String title) {
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setLevel(1))).hasText(title);
    }

    private void waitForUpdatesToConnect() {
        getHealthIndicator().waitUntilHealthy();
    }

    @NonNull
    private LinkHelper getSectionLink(String Trades) {
        return LinkHelper.getByLabel(getMainToolbar(), Trades);
    }

    private HealthIndicatorHelper getHealthIndicator() {
        return new HealthIndicatorHelper(getMainToolbar());
    }

    private Locator getMainToolbar() {
        return page.getByRole(AriaRole.TOOLBAR, new Page.GetByRoleOptions().setName("Main Sections")).first();
    }

    protected void assertWarningShown(String error) {
        NotificationHelper.findWarning(this.page.locator("body"))
                .verifyText(error);
    }

    protected void assertErrorShown(String error) {
        NotificationHelper.findError(this.page.locator("body"))
                .verifyText(error);
    }

    private void navigateToBookie() {
        page.navigate(baseUrl());
        waitForUpdatesToConnect();
    }
}
