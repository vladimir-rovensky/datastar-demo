package com.bookie.screens;

import com.bookie.domain.ReferenceDataRepository;
import com.bookie.infra.SessionRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.LocalDate;
import java.util.List;

@Component
public class TradeTicketPopup {

    private final ReferenceDataRepository referenceDataRepository;
    private final SessionRegistry sessionRegistry;

    public TradeTicketPopup(ReferenceDataRepository referenceDataRepository, SessionRegistry sessionRegistry) {
        this.referenceDataRepository = referenceDataRepository;
        this.sessionRegistry = sessionRegistry;
    }

    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/trades/buy", this::openBuyTicket)
                .POST("/trades/cancel", this::cancelTicket)
                .build();
    }

    private ServerResponse openBuyTicket(ServerRequest request) throws Exception {
        var channel = sessionRegistry.get(request).getClientChannel();
        channel.updateFragment(render(), "#trades-screen", "append");
        return ServerResponse.ok().build();
    }

    private ServerResponse cancelTicket(ServerRequest request) throws Exception {
        var channel = sessionRegistry.get(request).getClientChannel();
        channel.updateFragment("", "#popup", "remove");
        return ServerResponse.ok().build();
    }

    //language=HTML
    private String render() {
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
        var sb = new StringBuilder();
        for (String value : options) {
            sb.append("<option value=\"").append(value).append("\">").append(value).append("</option>");
        }
        return """
                <select name="%s">%s</select>
            """.formatted(name, sb.toString());
    }
}