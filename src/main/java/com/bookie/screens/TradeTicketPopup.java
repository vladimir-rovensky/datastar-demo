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
import static com.bookie.infra.Response.sse;
import static com.bookie.infra.TemplatingEngine.format;

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

    public ServerResponse close() {
        return Popup.close();
    }

    public ServerResponse onInput(ServerRequest request) throws ServletException, IOException {
        var ticket = request.body(Trade.class);
        return sse(channel -> handleInput(ticket, channel));
    }

    public ServerResponse onBookTrade(ServerRequest request) throws ServletException, IOException {
        var ticket = request.body(Trade.class);
        bookTicket(ticket);
        return close();
    }

    private void handleInput(Trade ticket, ClientChannel channel) {
        channel.updateFragment(this.render(ticket));

        var accrued = pricingService.calculateAccruedInterest(ticket.getCusip(), ticket.getQuantity());
        ticket.setAccruedInterest(accrued);
        channel.updateFragment(this.render(ticket));
        channel.complete();
    }

    //language=HTML
    public String render(Trade ticket) {
        var isModify = ticket.getId() != null;
        var btnClass = ticket.getDirection() == TradeDirection.BUY ? "btn-buy" : "btn-sell";
        var btnLabel = isModify ? "OK" : ticket.getDirection().getLabel();
        var title = isModify ? "Modify Trade" : "Book a Trade";
        var directionName = ticket.getDirection() != null ? ticket.getDirection().name() : null;

        var content = format("""
                <div class="form-fields" data-indicator:_fetching data-on:change="@post('/trades/input')">
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
                        .withInput(numberInput("quantity", ticket.getQuantity()))
                        .withError(tradeRepository.validateQuantity(ticket.getQuantity())),

                "accruedInterestField", formField("Accrued Interest ($)")
                        .withInput(numberInput("accruedInterest", ticket.getAccruedInterest())
                                .withLoadIndicator("_fetching")
                                .withDisabled(true)),

                "tradeDate", formField("Trade Date")
                        .withInput(dateInput("tradeDate", ticket.getTradeDate())
                                .withDisabled(true)),

                "settleDate", formField("Settle Date")
                        .withInput(dateInput("settleDate", ticket.getSettleDate())));

        var actions = format("""
                <button class="${btnClass}" data-on:click="@post('/trades/book')">${btnLabel}</button>
                <button data-on:click="@post('/trades/cancel')">Cancel</button>
                """,
                "btnClass", btnClass,
                "btnLabel", btnLabel);

        return popup()
                .withTitle(title)
                .withContent(content)
                .withActions(actions)
                .render();
    }

    private void bookTicket(Trade ticket) {
        if (!tradeRepository.isValid(ticket)) {
            throw new RuntimeException("Tried to book an invalid ticket.");
        }

        if (ticket.getId() != null) {
            tradeRepository.modifyTrade(ticket);
        } else {
            tradeRepository.bookTrade(ticket);
        }
    }
}