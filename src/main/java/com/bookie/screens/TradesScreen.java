package com.bookie.screens;

import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import com.bookie.domain.entity.TradeRepository;
import com.bookie.infra.ClientChannel;
import com.bookie.infra.MessageBus;
import com.bookie.infra.SessionRegistry;
import com.bookie.infra.events.TradeBookedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import static com.bookie.infra.TemplatingEngine.format;
import static com.bookie.screens.Shell.shell;

@Configuration
public class TradesScreen extends BaseScreen {

    private final TradeRepository tradeRepository;
    private final TradeTicketPopup tradeTicketPopup;
    private final SessionRegistry sessionRegistry;
    private final MessageBus messageBus;

    private List<Trade> trades;

    public TradesScreen(TradeRepository tradeRepository, TradeTicketPopup tradeTicketPopup,
                        SessionRegistry sessionRegistry, MessageBus messageBus) {

        this.tradeRepository = tradeRepository;
        this.tradeTicketPopup = tradeTicketPopup;
        this.sessionRegistry = sessionRegistry;
        this.messageBus = messageBus;

        messageBus.subscribe(TradeBookedEvent.class, this::onTradeBooked);
    }

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .GET("", req -> sessionRegistry
                        .createSession(TradesScreen.class)
                        .getScreen(TradesScreen.class)
                        .initialRender(req))
                .POST("buy", req -> sessionRegistry
                        .getScreen(req, TradesScreen.class)
                        .openBuyTicket(req))
                .POST("sell", req -> sessionRegistry
                        .getScreen(req, TradesScreen.class)
                        .openSellTicket(req))
                .POST("cancel", req -> sessionRegistry
                        .getScreen(req, TradesScreen.class)
                        .getTradeTicketPopup()
                        .onCancel(req))
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

    public ServerResponse initialRender(ServerRequest request) {
        this.trades = tradeRepository.getAllTrades();

        return html(render());
    }

    public ServerResponse openBuyTicket(ServerRequest request) {
        tradeTicketPopup.setDirection(TradeDirection.BUY);
        tradeTicketPopup.setVisible(true);
        var channel = sessionRegistry.getSession(request).getClientChannel();
        channel.updateFragment(tradeTicketPopup.render(), "#trades-screen", "append");
        return ServerResponse.ok().build();
    }

    public ServerResponse openSellTicket(ServerRequest request) {
        tradeTicketPopup.setDirection(TradeDirection.SELL);
        tradeTicketPopup.setVisible(true);
        var channel = sessionRegistry.getSession(request).getClientChannel();
        channel.updateFragment(tradeTicketPopup.render(), "#trades-screen", "append");
        return ServerResponse.ok().build();
    }

    public TradeTicketPopup getTradeTicketPopup() {
        return tradeTicketPopup;
    }

    public String render() {
        return shell(this.tabID)
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
                                <th>Direction</th>
                                <th>Quantity</th>
                                <th>Trade Date</th>
                                <th>Settle Date</th>
                                <th>Accrued Interest</th>
                                <th>Book</th>
                                <th>Counterparty</th>
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
                        <tr>
                            <td>${id}</td>
                            <td>${cusip}</td>
                            <td>${direction}</td>
                            <td>${quantity}</td>
                            <td>${tradeDate}</td>
                            <td>${settleDate}</td>
                            <td>${accruedInterest}</td>
                            <td>${book}</td>
                            <td>${counterparty}</td>
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
        getClientChannel().updateFragment(this.render());
    }

    private ClientChannel getClientChannel() {
        return this.sessionRegistry.getSession(tabID)
                .getClientChannel();
    }

    private static String usd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

}