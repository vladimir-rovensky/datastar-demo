package com.bookie.screens;

import com.bookie.domain.TradeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

import static com.bookie.screens.Shell.shell;

@Configuration
public class TradesScreen {

    private final TradeRepository tradeRepository;

    public TradesScreen(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Bean
    public RouterFunction<ServerResponse> tradesRoutes() {
        return RouterFunctions.route()
                .GET("/trades", _ -> html(render()))
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
                    <div class="trades-screen">
                    <h1>Trades</h1>
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
        return tradeRepository.findAll().stream()
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
}