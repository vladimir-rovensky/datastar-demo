package com.bookie.screens;

import com.bookie.components.Popup;
import com.bookie.domain.entity.ReferenceDataRepository;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import com.bookie.domain.entity.TradeRepository;
import com.bookie.domain.service.PricingService;
import com.bookie.infra.ClientChannel;
import jakarta.servlet.ServletException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.util.List;

import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
import static com.bookie.components.Popup.popup;
import com.bookie.infra.EscapedHtml;
import static com.bookie.infra.Response.sse;
import static com.bookie.infra.TemplatingEngine.html;

@Component
public class TradeTicketPopup {

    private final ReferenceDataRepository referenceDataRepository;
    private final PricingService pricingService;
    private final TradeRepository tradeRepository;

    public TradeTicketPopup(ReferenceDataRepository referenceDataRepository, PricingService pricingService,
                            TradeRepository tradeRepository) {

        this.referenceDataRepository = referenceDataRepository;
        this.pricingService = pricingService;
        this.tradeRepository = tradeRepository;
    }

    public RouterFunction<ServerResponse> setupRoutes() {
        return RouterFunctions.route()
                .POST("buy", _ -> this.openBuyTicket())
                .POST("sell", _ -> this.openSellTicket())
                .POST("cancel", _ -> this.close())
                .POST("input", this::onInput)
                .POST("book", this::onBookTrade)
                .build();
    }

    public ServerResponse openBuyTicket() {
        return Popup.open(this.render(Trade.aBlankTrade(TradeDirection.BUY)));
    }

    public ServerResponse openSellTicket() {
        return Popup.open(this.render(Trade.aBlankTrade(TradeDirection.SELL)));
    }

    public ServerResponse close() {
        return Popup.close();
    }

    public ServerResponse onInput(ServerRequest request) throws ServletException, IOException {
        var ticket = request.body(Trade.class);
        return sse(channel -> handleInput(ticket, channel));
    }

    public ServerResponse onBookTrade(ServerRequest request) throws ServletException, IOException {
        var trade = request.body(Trade.class);

        if (!tradeRepository.bookTrade(trade)) {
            return sse(channel -> channel.updateFragment(render(trade)));
        }

        return close();
    }

    private void handleInput(Trade ticket, ClientChannel channel) {
        channel.updateFragment(this.render(ticket));

        var accrued = pricingService.calculateAccruedInterest(ticket.getCusip(), ticket.getQuantity());
        ticket.setAccruedInterest(accrued);
        channel.updateFragment(this.render(ticket));
    }

    public static EscapedHtml getToolbarButtons() {
        return html("""
                <button class="btn-large btn-buy" data-on:click="@post('/tradeTicket/buy')" data-tooltip='Book a new BUY Trade'>B</button>
                <button class="btn-large btn-sell" data-on:click="@post('/tradeTicket/sell')" data-tooltip='Book a new SELL Trade'>S</button>
                """);
    }

    public EscapedHtml render(Trade ticket) {
        var isModify = ticket.getId() != null;
        var btnClass = ticket.getDirection() == TradeDirection.BUY ? "btn-buy" : "btn-sell";
        var btnLabel = isModify ? "OK" : ticket.getDirection().getLabel();
        var title = isModify ? "Modify Trade" : "Book a Trade";
        var directionName = ticket.getDirection() != null ? ticket.getDirection().name() : null;
        var isValid = tradeRepository.isValid(ticket);

        var content = html("""
                <div class="form-fields" data-indicator:_fetching data-signals="{id: ${tradeId}}" data-on:change="@post('/tradeTicket/input')">
                    ${cusip}
                    ${book}
                    ${type}
                    ${counterparty}
                    ${quantity}
                    ${accruedInterestField}
                    ${tradeDate}
                    ${settleDate}
                </div>
                """,

                "tradeId", ticket.getId(),

                "cusip", formField("CUSIP")
                        .withInput(textInput("cusip", ticket.getCusip()))
                        .withError(tradeRepository.validateCusip(ticket.getCusip())),

                "book", formField("Book")
                        .withInput(selectInput("book", referenceDataRepository.getAllBooks(), ticket.getBook()))
                        .withError(tradeRepository.validateBook(ticket.getBook())),

                "type", formField("Type")
                        .withInput(selectInput("direction", List.of("BUY", "SELL"), directionName)),

                "counterparty", formField("Counterparty")
                        .withInput(selectInput("counterparty", referenceDataRepository.getAllCounterparties(), ticket.getCounterparty()))
                        .withError(tradeRepository.validateCounterparty(ticket.getCounterparty())),

                "quantity", formField("Quantity ($)")
                        .withInput(numberInput("quantity", ticket.getQuantity())
                                .withFormat("currency"))
                        .withError(tradeRepository.validateQuantity(ticket)),

                "accruedInterestField", formField("Accrued Interest ($)")
                        .withInput(numberInput("accruedInterest", ticket.getAccruedInterest())
                                .withFormat("currency")
                                .withLoadIndicator("_fetching")
                                .withDisabled(true)),

                "tradeDate", formField("Trade Date")
                        .withInput(dateInput("tradeDate", ticket.getTradeDate())
                                .withDisabled(true)),

                "settleDate", formField("Settle Date")
                        .withInput(dateInput("settleDate", ticket.getSettleDate())));

        var actions = html("""
                <button class="btn-large ${btnClass}" data-on:click="@post('/tradeTicket/book')" data-attr:disabled="$_fetching || !${valid}">${btnLabel}</button>
                <button data-on:click="@post('/tradeTicket/cancel')">Cancel</button>
                """,
                "valid", isValid,
                "btnClass", btnClass,
                "btnLabel", btnLabel);

        return popup()
                .withTitle(title)
                .withContent(content)
                .withActions(actions)
                .render();
    }

}
