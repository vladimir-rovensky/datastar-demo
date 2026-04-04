package com.bookie.screens;

import com.bookie.domain.entity.ReferenceDataRepository;
import com.bookie.domain.service.PricingService;
import com.bookie.infra.ClientChannel;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.LocalDate;
import java.util.List;

@Component
public class TradeTicketPopup extends BaseScreen{

    private final ReferenceDataRepository referenceDataRepository;
    private final PricingService pricingService;

    public TradeTicketPopup(ReferenceDataRepository referenceDataRepository, PricingService pricingService) {
        this.referenceDataRepository = referenceDataRepository;
        this.pricingService = pricingService;
    }

    public RouterFunction<ServerResponse> routes() {
        return RouterFunctions.route()
                .POST("/trades/cancel", this::cancelTicket)
                .POST("/trades/input", this::handleInput)
                .build();
    }

    private ServerResponse cancelTicket(ServerRequest request) {
        return ServerResponse.sse(b -> {
            var channel = new ClientChannel();
            channel.connect(b);
            channel.updateFragment("", "#popup", "remove");
            channel.complete();
        });
    }

    private ServerResponse handleInput(ServerRequest request) throws Exception {
        var ticket = request.body(TradeTicket.class);

        return ServerResponse.sse(b -> {
            var channel = new ClientChannel();
            channel.connect(b);
            channel.updateFragment(this.render(ticket));

            pricingService.calculateAccruedInterest(ticket.getCusip(), ticket.getQuantity())
                    .thenAccept(accrued -> {
                        ticket.setAccruedInterest(accrued);
                        channel.updateFragment(this.render(ticket));
                        channel.complete();
                    });
        });
    }

    public String render() {
        var ticket = new TradeTicket();
        ticket.setTradeDate(LocalDate.now());
        ticket.setSettleDate(LocalDate.now().plusDays(2));
        return render(ticket);
    }

    //language=HTML
    private String render(TradeTicket ticket) {
        boolean cusipBlank = ticket.getCusip() == null || ticket.getCusip().isBlank();
        return """
                <div id="popup" class="popup-overlay">
                    <div class="popup" data-signals="{accruedInterest: %s}">
                        <div class="popup-title">Buy Ticket</div>
                        <div class="form-fields" data-on:change="@post('/trades/input')" data-indicator:fetching>
                            <label>CUSIP<div class="input-wrapper" %s><input type="text" name="cusip" data-bind="cusip" value="%s" autocomplete='off'></div></label>
                            <label>Book%s</label>
                            <label>Quantity ($)<input type="number" name="quantity" data-bind="quantity" value="%s"  onfocus="this.select()"></label>
                            <label>Accrued Interest ($)
                                <div class="loading-field">
                                    <input type="number" name="accruedInterest" data-bind="accruedInterest" value="%s" data-style-opacity="$fetching ? '0' : '1'" disabled>
                                    <div class="loading-field-icon" data-show="$fetching"><div class="spinner"></div></div>
                                </div>
                            </label>
                            <label>Trade Date<input type="date" name="tradeDate" data-bind="tradeDate" value="%s" disabled></label>
                            <label>Settle Date<input type="date" name="settleDate" data-bind="settleDate" value="%s"></label>
                            <label>Counterparty%s</label>
                        </div>
                        <div class="popup-actions">
                            <button class="btn-buy">Buy</button>
                            <button data-on:click="@post('/trades/cancel')">Cancel</button>
                        </div>
                    </div>
                </div>
                """.formatted(
                ticket.getAccruedInterest(),
                cusipBlank ? " data-error=\"This field is required\"" : "",
                orEmpty(ticket.getCusip()),
                select("book", referenceDataRepository.getAllBooks(), ticket.getBook()),
                orEmpty(ticket.getQuantity()),
                orEmpty(ticket.getAccruedInterest()),
                orEmpty(ticket.getTradeDate()),
                orEmpty(ticket.getSettleDate()),
                select("counterparty", referenceDataRepository.getAllCounterparties(), ticket.getCounterparty()));
    }

    //language=HTML
    private String select(String name, List<String> options, String selected) {
        var sb = new StringBuilder();
        for (String value : options) {
            if (value.equals(selected)) {
                sb.append("<option value=\"").append(value).append("\" selected>").append(value).append("</option>");
            } else {
                sb.append("<option value=\"").append(value).append("\">").append(value).append("</option>");
            }
        }
        return """
                <select name="%s" data-bind="%s">%s</select>
            """.formatted(name, name, sb.toString());
    }

    private static String orEmpty(Object value) {
        return value != null ? value.toString() : "";
    }

}