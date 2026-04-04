package com.bookie.screens;

import com.bookie.domain.entity.TradeRepository;
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
import java.util.Locale;

import static com.bookie.screens.Shell.shell;

@Configuration
public class TradesScreen extends BaseScreen {

    private final TradeRepository tradeRepository;
    private final TradeTicketPopup tradeTicketPopup;
    private final SessionRegistry sessionRegistry;
    private final MessageBus messageBus;

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
        return html(render(this.tabID));
    }

    public ServerResponse openBuyTicket(ServerRequest request) throws Exception {
        var channel = sessionRegistry.getSession(request).getClientChannel();
        channel.updateFragment(tradeTicketPopup.render(), "#trades-screen", "append");
        return ServerResponse.ok().build();
    }

    public TradeTicketPopup getTradeTicketPopup() {
        return tradeTicketPopup;
    }

    public String render(String tabId) {
        return shell(tabId)
                .withTitle("Trades")
                .withContent(getContent())
                .render();
    }

    //language=HTML
    private String getContent() {
        return """
                    <div id="trades-screen" class="trades-screen">
                    <div class="toolbar">
                        <button class="btn-buy" data-on:click="@post('/trades/buy')">B</button>
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
                        %s
                        </tbody>
                    </table>
                    </div>
                """.formatted(getTradeRows());
    }

    //language=HTML
    private String getTradeRows() {
        return tradeRepository.getAllTrades().reversed().stream()
                .map(t -> """
                        <tr>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                            <td>%s</td>
                        </tr>
                        """.formatted(
                        t.getId(), t.getCusip(), t.getDirection(),
                        usd(t.getQuantity()), t.getTradeDate(), t.getSettleDate(),
                        usd(t.getAccruedInterest()), t.getBook(), t.getCounterparty()))
                .reduce("", String::concat);
    }

    private void onTradeBooked(TradeBookedEvent event) {
        //TODO
    }

    private static String usd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

}