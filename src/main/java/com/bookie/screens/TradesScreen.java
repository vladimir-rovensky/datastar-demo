package com.bookie.screens;

import com.bookie.domain.entity.*;
import com.bookie.domain.service.PricingService;
import com.bookie.infra.MessageBus;
import com.bookie.infra.SessionRegistry;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static com.bookie.infra.Response.html;
import static com.bookie.infra.TemplatingEngine.format;
import static com.bookie.screens.Shell.shell;

@Configuration
public class TradesScreen extends BaseScreen {

    private final TradeRepository tradeRepository;
    private final ReferenceDataRepository referenceDataRepository;
    private final PricingService pricingService;
    private final BondRepository bondRepository;

    private List<Trade> trades;
    private TradeTicketPopup tradeTicketPopup;
    private final Runnable unsubscribeFromTradeBooked;
    private final Runnable unsubscribeFromTradeModified;

    public TradesScreen(TradeRepository tradeRepository, ReferenceDataRepository referenceDataRepository,
                        PricingService pricingService, BondRepository bondRepository,
                        SessionRegistry sessionRegistry, MessageBus messageBus) {
        super(sessionRegistry);

        this.tradeRepository = tradeRepository;
        this.referenceDataRepository = referenceDataRepository;
        this.pricingService = pricingService;
        this.bondRepository = bondRepository;

        this.unsubscribeFromTradeBooked = messageBus.subscribe(TradeBookedEvent.class, this::onTradeBooked);
        this.unsubscribeFromTradeModified = messageBus.subscribe(TradeModifiedEvent.class, this::onTradeModified);
    }

    @Override
    public void dispose() {
        this.unsubscribeFromTradeBooked.run();
        this.unsubscribeFromTradeModified.run();
    }

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .GET("", _ -> sessionRegistry
                        .createSession(TradesScreen.class)
                        .getScreen(TradesScreen.class)
                        .initialRender())
                .POST("buy", req -> sessionRegistry
                        .getScreen(req, TradesScreen.class)
                        .openBuyTicket())
                .POST("sell", req -> sessionRegistry
                        .getScreen(req, TradesScreen.class)
                        .openSellTicket())
                .POST("modify/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .openModifyTicket(request))
                .POST("cancel", req -> sessionRegistry
                        .getScreen(req, TradesScreen.class)
                        .getTradeTicketPopup()
                        .close())
                .POST("input", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .getTradeTicketPopup()
                        .onInput(request))
                .POST("book", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .getTradeTicketPopup()
                        .onBookTrade(request))
                .build();
    }

    public ServerResponse initialRender() {
        this.trades = tradeRepository.getAllTrades();

        return html(render());
    }

    public ServerResponse openBuyTicket() {
        tradeTicketPopup = createTicketPopup();
        tradeTicketPopup.setDirection(TradeDirection.BUY);
        showTradeTicket(tradeTicketPopup.render());
        return ServerResponse.ok().build();
    }

    public ServerResponse openSellTicket() {
        tradeTicketPopup = createTicketPopup();
        tradeTicketPopup.setDirection(TradeDirection.SELL);
        showTradeTicket(tradeTicketPopup.render());
        return ServerResponse.ok().build();
    }

    public ServerResponse openModifyTicket(ServerRequest request) {
        var tradeID = Long.parseLong(request.pathVariable("id"));
        var trade = tradeRepository.findById(tradeID);
        tradeTicketPopup = createTicketPopup();
        showTradeTicket(tradeTicketPopup.render(TradeTicket.fromTrade(trade)));
        return ServerResponse.ok().build();
    }

    private TradeTicketPopup createTicketPopup() {
        return new TradeTicketPopup(referenceDataRepository, pricingService, bondRepository, tradeRepository);
    }

    public String render() {
        return shell(this.getTabID())
                .withTitle("Trades")
                .withContent(getContent())
                .render();
    }

    //language=HTML
    private String getContent() {
        return format("""
                    <div id="trades-screen" class="trades-screen">
                    <div class="toolbar">
                        <button class="btn-buy" data-on:click="@post('/trades/buy')">B</button>
                        <button class="btn-sell" data-on:click="@post('/trades/sell')">S</button>
                        <span class="toolbar-title">Trades</span>
                    </div>
                    <table>
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>CUSIP</th>
                                <th>Book</th>
                                <th>Type</th>
                                <th>Counterparty</th>
                                <th>Quantity</th>
                                <th>Accrued Interest</th>
                                <th>Trade Date</th>
                                <th>Settle Date</th>
                            </tr>
                        </thead>
                        <tbody>
                        ${rows}
                        </tbody>
                    </table>
                    </div>
                """,
                "rows", getTradeRows());
    }

    //language=HTML
    private String getTradeRows() {
        return this.trades.reversed().stream()
                .map(t -> format("""
                        <tr data-on:dblclick="@post('/trades/modify/${id}')">
                            <td>${id}</td>
                            <td>${cusip}</td>
                            <td>${book}</td>
                            <td>${direction}</td>
                            <td>${counterparty}</td>
                            <td>${quantity}</td>
                            <td>${accruedInterest}</td>
                            <td>${tradeDate}</td>
                            <td>${settleDate}</td>
                        </tr>
                        """,
                        "id", t.getId(),
                        "cusip", t.getCusip(),
                        "direction", t.getDirection(),
                        "quantity", usd(t.getQuantity()),
                        "tradeDate", t.getTradeDate(),
                        "settleDate", t.getSettleDate(),
                        "accruedInterest", usd(t.getAccruedInterest()),
                        "book", t.getBook(),
                        "counterparty", t.getCounterparty()))
                .reduce("", String::concat);
    }

    private void onTradeBooked(TradeBookedEvent event) {
        this.trades.add(event.getTrade());
        getUpdateChannel().updateFragment(this.render());
    }

    private void onTradeModified(TradeModifiedEvent event) {
        this.trades.replaceAll(t -> t.getId().equals(event.getTrade().getId()) ? event.getTrade() : t);
        getUpdateChannel().updateFragment(this.render());
    }

    private void showTradeTicket(String content) {
        getUpdateChannel().appendFragment(content, "#trades-screen");
    }

    private TradeTicketPopup getTradeTicketPopup() {
        return tradeTicketPopup;
    }

    private static String usd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

}