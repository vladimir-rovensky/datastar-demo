package com.bookie.screens;

import com.bookie.components.Popup;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeRepository;
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

@Configuration
public class TradesScreen extends BaseScreen {

    private final TradeRepository tradeRepository;
    private final TradeTicketPopup tradeTicketPopup;

    private List<Trade> trades;
    private final Runnable unsubscribeFromTradeBooked;
    private final Runnable unsubscribeFromTradeModified;

    public TradesScreen(TradeRepository tradeRepository, TradeTicketPopup tradeTicketPopup,
                        SessionRegistry sessionRegistry, MessageBus messageBus) {
        super("Trades", sessionRegistry);

        this.tradeRepository = tradeRepository;
        this.tradeTicketPopup = tradeTicketPopup;

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
                .POST("modify/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .openModifyTicket(request))
                .build();
    }

    public ServerResponse initialRender() {
        this.trades = tradeRepository.getAllTrades();

        return html(render());
    }

    public ServerResponse openModifyTicket(ServerRequest request) {
        var tradeID = Long.parseLong(request.pathVariable("id"));
        var trade = tradeRepository.findById(tradeID);
        return Popup.open(tradeTicketPopup.render(trade));
    }


    //language=HTML
    @Override
    protected String getContent() {
        return format("""
                    <div id="trades-screen" class="trades-screen">
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
        reRender();
    }

    private void onTradeModified(TradeModifiedEvent event) {
        this.trades.replaceAll(t -> t.getId().equals(event.getTrade().getId()) ? event.getTrade() : t);
        reRender();
    }

    private static String usd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

}