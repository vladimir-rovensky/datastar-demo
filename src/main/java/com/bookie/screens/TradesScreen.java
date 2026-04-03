package com.bookie.screens;

import com.bookie.domain.ReferenceDataRepository;
import com.bookie.domain.TradeRepository;
import com.bookie.infra.SessionRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static com.bookie.screens.Shell.shell;

@Configuration
public class TradesScreen {

    private final TradeRepository tradeRepository;
    private final ReferenceDataRepository referenceDataRepository;
    private final SessionRegistry sessionRegistry;

    public TradesScreen(TradeRepository tradeRepository, ReferenceDataRepository referenceDataRepository, SessionRegistry sessionRegistry) {
        this.tradeRepository = tradeRepository;
        this.referenceDataRepository = referenceDataRepository;
        this.sessionRegistry = sessionRegistry;
    }

    @Bean
    public RouterFunction<ServerResponse> tradesRoutes() {
        return RouterFunctions.route()
                .GET("/trades", _ -> html(render()))
                .POST("/trades/buy", this::openBuyTicket)
                .POST("/trades/cancel", this::cancelTicket)
                .build();
    }

    private ServerResponse html(String content) {
        return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(content);
    }

    private String render() {
        return shell()
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
        return tradeRepository.getAllTrades().stream()
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

    private static String usd(BigDecimal amount) {
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    private ServerResponse cancelTicket(ServerRequest request) throws Exception {
        var channel = sessionRegistry.get(request).getClientChannel();
        channel.updateFragment("", "#popup", "remove");
        return ServerResponse.ok().build();
    }

    private ServerResponse openBuyTicket(ServerRequest request) throws Exception {
        var channel = sessionRegistry.get(request).getClientChannel();
        channel.updateFragment(getTradeTicket(), "#trades-screen", "append");
        return ServerResponse.ok().build();
    }

    //language=HTML
    private String getTradeTicket() {
        LocalDate tradeDate = LocalDate.now();
        LocalDate settleDate = tradeDate.plusDays(2);

        return """
                <div id="popup" class="popup-overlay">
                    <div class="popup">
                        <div class="popup-title">Buy Ticket</div>
                        <div class="form-fields">
                            <label>CUSIP<input type="text" name="cusip"></label>
                            <label>Book%s</label>
                            <label>Quantity<input type="number" name="quantity"></label>
                            <label>Accrued Interest<input type="number" name="accruedInterest"></label>
                            <label>Trade Date<input type="date" name="tradeDate" value="%s" disabled></label>
                            <label>Settle Date<input type="date" name="settleDate" value="%s"></label>
                            <label>Counterparty%s</label>
                        </div>
                        <div class="popup-actions">
                            <button class="btn-buy">Buy</button>
                            <button data-on:click="@post('/trades/cancel')">Cancel</button>
                        </div>
                    </div>
                </div>
                """.formatted(
                select("book", referenceDataRepository.getAllBooks()),
                tradeDate,
                settleDate,
                select("counterparty", referenceDataRepository.getAllCounterparties()));
    }

    //language=HTML
    private String select(String name, List<String> options) {
        var optionsBuilder = new StringBuilder();
        for (String value : options) {
            optionsBuilder.append("<option value=\"").append(value).append("\">").append(value).append("</option>");
        }

        return """
                <select name="%s">%s</select>
            """.formatted(name, optionsBuilder.toString());
    }

}