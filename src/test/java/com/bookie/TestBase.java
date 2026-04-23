package com.bookie;

import com.bookie.domain.entity.*;
import com.bookie.domain.service.PositionService;
import com.bookie.infra.*;
import com.bookie.screens.Page;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.*;

import java.nio.file.Paths;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class TestBase {

    private JettyBootstrap server;
    private final boolean isolateTests = false; //False -> faster, but tests use the same tab/context
    private BrowserContext browserContext;
    protected com.bookie.screens.Page page;

    @BeforeAll
    void globaSetup() throws Exception {
        server = JettyBootstrap.start();

        if(!isolateTests) {
            this.browserContext = createContext();
            this.page = setupPage();
        }
    }

    @AfterAll
    void globalTeardown() throws Exception {
        if(!isolateTests) {
            tearDownContext();
        }

        server.stop();
    }

    @BeforeEach
    void setup() {
        getSessionRegistry().reset();
        getBondDAO().reset();
        getTradeDAO().reset();
        getPositionService().reset();

        if(isolateTests) {
            this.browserContext = createContext();
            this.page = setupPage();
        }

        page.navigateToBookie();
    }

    @AfterEach
    void teardown(TestInfo ignored) {
        if(isolateTests) {
            tearDownContext();
        }
    }

    protected Page openNewTab() {
        return setupPage()
                .navigateToBookie();
    }

    private Page setupPage() {
        return new Page(browserContext.newPage(), getBaseUrl());
    }

    private BrowserContext createContext() {
        return PlaywrightManager.getBrowser().newContext();
    }

    private void tearDownContext() {
        browserContext.close();
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

    protected String getBaseUrl() {
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

    protected void trace(Runnable test) {
        trace(test, "trace.zip");
    }

    protected void trace(Runnable test, String fileName) {
        try {
            startTracing();
            page.navigateToBookie();
            test.run();
        } finally {
            stopTracing(fileName);
        }
    }

    private void startTracing() {
        this.browserContext.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
    }

    private void stopTracing(String filename) {
        this.browserContext.tracing().stop(new Tracing.StopOptions()
                .setPath(Paths.get(filename)));
    }

}
