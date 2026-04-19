package com.bookie;

import com.bookie.domain.entity.*;
import com.bookie.infra.JettyBootstrap;
import com.bookie.infra.PlaywrightManager;
import com.bookie.infra.SessionRegistry;
import com.bookie.screens.trades.TradesScreenPageObject;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BookieE2ETest {

    private JettyBootstrap server;
    private BrowserContext browserContext;
    protected Page page;

    @BeforeAll
    void startServer() throws Exception {
        server = JettyBootstrap.start();
    }

    @AfterAll
    void stopServer() throws Exception {
        server.stop();
    }

    @BeforeEach
    void resetState() {
        getSessionRegistry().clearAll();
        getBondDAO().clear();
        getTradeDAO().clear();
        browserContext = PlaywrightManager.getBrowser().newContext();
        page = browserContext.newPage();
        page.setDefaultTimeout(5000);
    }

    @AfterEach
    void closeBrowserContext() {
        browserContext.close();
    }

    protected HttpClient getHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    protected void trace(Runnable test) {
        try {
            startTracing();
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

    protected FakeBondDAO getBondDAO() {
        return server.getBean(FakeBondDAO.class);
    }

    private SessionRegistry getSessionRegistry() {
        return server.getBean(SessionRegistry.class);
    }

    protected String baseUrl() {
        return "http://localhost:" + server.getPort();
    }

    protected void givenExistingTrades(Trade... trades) {
        givenExistingTrades(List.of(trades));
    }

    protected void givenExistingTrades(List<Trade> trades) {
        getTradeDAO().saveAll(trades);
    }

    protected void givenExistingBonds(Bond... bonds) {
        givenExistingBonds(List.of(bonds));
    }

    protected void givenExistingBonds(List<Bond> bonds) {
        getBondDAO().saveAll(bonds);
    }

    @NonNull
    protected static Trade aTrade(Long id, String cusip, TradeDirection tradeDirection, int quantity) {
        Trade trade = new Trade();
        trade.setId(id);
        trade.setCusip(cusip);
        trade.setDirection(tradeDirection);
        trade.setQuantity(BigDecimal.valueOf(quantity));
        trade.setBook("CREDIT-NY");
        trade.setCounterparty("GOLDMAN");
        trade.setTradeDate(LocalDate.of(2026, 1, 15));
        trade.setSettleDate(LocalDate.of(2026, 1, 17));
        return trade;
    }

    @NonNull
    protected static Bond aBond(String cusip) {
        Bond bond = new Bond();
        bond.setCusip(cusip);
        bond.setDescription("US Treasury 2.5% 2030");
        return bond;
    }

    protected TradesScreenPageObject switchToTrades() {
        page.navigate(baseUrl() + "/trades");
        return new TradesScreenPageObject(page);
    }
}
