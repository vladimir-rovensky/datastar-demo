package com.bookie.screens;

import com.bookie.domain.TradeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

@Configuration
public class TradesScreen {

    private final TradeRepository tradeRepository;

    public TradesScreen(TradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Bean
    public RouterFunction<ServerResponse> tradesRoutes() {
        return RouterFunctions.route()
                .GET("/trades", _ -> handle())
                .build();
    }

    //language=HTML
    private ServerResponse handle() {
        String rows = tradeRepository.findAll().stream()
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
                        t.getQuantity().toPlainString(), t.getTradeDate(), t.getSettleDate(),
                        t.getAccruedInterest().toPlainString(), t.getBook(), t.getCounterparty()))
                .reduce("", String::concat);

        String html = """
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
                """ + rows + """
                    </tbody>
                </table>
                """;

        return ServerResponse.ok().contentType(MediaType.TEXT_HTML).body(html);
    }
}