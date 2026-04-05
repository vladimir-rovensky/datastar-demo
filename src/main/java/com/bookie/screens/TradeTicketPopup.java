package com.bookie.screens;

import com.bookie.domain.entity.BondRepository;
import com.bookie.domain.entity.ReferenceDataRepository;
import com.bookie.domain.entity.TradeDirection;
import com.bookie.domain.entity.TradeRepository;
import com.bookie.domain.service.PricingService;
import com.bookie.infra.ClientChannel;
import jakarta.servlet.ServletException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
import static com.bookie.infra.Response.removeFragment;
import static com.bookie.infra.Response.sse;
import static com.bookie.infra.TemplatingEngine.format;

@Component
public class TradeTicketPopup {

    private TradeDirection direction = TradeDirection.BUY;
    private boolean visible;

    private final ReferenceDataRepository referenceDataRepository;
    private final PricingService pricingService;
    private final BondRepository bondRepository;
    private final TradeRepository tradeRepository;

    public TradeTicketPopup(ReferenceDataRepository referenceDataRepository, PricingService pricingService,
                            BondRepository bondRepository, TradeRepository tradeRepository) {

        this.referenceDataRepository = referenceDataRepository;
        this.pricingService = pricingService;
        this.bondRepository = bondRepository;
        this.tradeRepository = tradeRepository;
        this.visible = true;
    }

    public void setDirection(TradeDirection direction) {
        this.direction = direction;
    }

    public ServerResponse close() {
        this.visible = false;
        return removeFragment("#popup");
    }

    public ServerResponse onInput(ServerRequest request) throws ServletException, IOException {
        var ticket = request.body(TradeTicket.class);
        return sse(channel -> handleInput(ticket, channel));
    }

    public ServerResponse onBookTrade(ServerRequest request) throws ServletException, IOException {
        var ticket = request.body(TradeTicket.class);
        bookTicket(ticket);
        return close();
    }

    private void handleInput(TradeTicket ticket, ClientChannel channel) {
        channel.updateFragment(this.render(ticket));

        pricingService.calculateAccruedInterest(ticket.getCusip(), ticket.getQuantity())
                .thenAccept(accrued -> {
                    ticket.setAccruedInterest(accrued);
                    if(this.visible) {
                        channel.updateFragment(this.render(ticket));
                    }
                    channel.complete();
                });
    }

    public String render() {
        var ticket = new TradeTicket();
        ticket.setDirection(direction);
        ticket.setTradeDate(LocalDate.now());
        ticket.setSettleDate(LocalDate.now().plusDays(2));
        return render(ticket);
    }

    //language=HTML
    public String render(TradeTicket ticket) {
        var isModify = ticket.getTradeId() != null;
        var btnClass = ticket.getDirection() == TradeDirection.BUY ? "btn-buy" : "btn-sell";
        var btnLabel = isModify ? "OK" : ticket.getDirection().getLabel();
        var title = isModify ? "Modify Trade" : "Book a Trade";
        var directionName = ticket.getDirection() != null ? ticket.getDirection().name() : null;
        var tradeIdSignal = isModify ? ticket.getTradeId().toString() : "null";

        return format("""
                <div id="popup" class="popup-overlay" data-signals="{tradeId: ${tradeIdSignal}, accruedInterest: ${accruedInterest}}">
                    <div class="popup">
                        <div class="popup-title">${title}</div>
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
                        <div class="popup-actions">
                            <button class="${btnClass}" data-on:click="@post('/trades/book')">${btnLabel}</button>
                            <button data-on:click="@post('/trades/cancel')">Cancel</button>
                        </div>
                    </div>
                    <script type="module">
                        const popup = document.getElementById('popup').querySelector('.popup');
                        const title = popup.querySelector('.popup-title');
                        let position = {dx: 0, dy: 0};
                        popup.style.transform = `translate(${position.dx}px,${position.dy}px)`;
                        title.style.cursor = 'move';
                        title.addEventListener('pointerdown', e => {
                            e.currentTarget.setPointerCapture(e.pointerId);
                            const onMove = e => {
                                position.dx += e.movementX;
                                position.dy += e.movementY;
                                popup.style.transform = `translate(${position.dx}px,${position.dy}px)`;
                            };
                            title.addEventListener('pointermove', onMove);
                            title.addEventListener('pointerup', () => title.removeEventListener('pointermove', onMove), {once: true});
                        });
                    </script>
                </div>
                """,
                "tradeIdSignal", tradeIdSignal,
                "title", title,
                "btnClass", btnClass,
                "btnLabel", btnLabel,

                "cusip", formField("CUSIP")
                        .withInput(textInput("cusip", ticket.getCusip()))
                        .withError(validateCusip(ticket.getCusip())),

                "book", formField("Book")
                        .withInput(selectInput("book", referenceDataRepository.getAllBooks(), ticket.getBook())),

                "type", formField("Type")
                        .withInput(selectInput("direction", List.of("BUY", "SELL"), directionName)),

                "counterparty", formField("Counterparty")
                        .withInput(selectInput("counterparty", referenceDataRepository.getAllCounterparties(), ticket.getCounterparty())),

                "quantity", formField("Quantity ($)")
                        .withInput(numberInput("quantity", ticket.getQuantity()))
                        .withError(validateQuantity(ticket.getQuantity())),

                "accruedInterest", ticket.getAccruedInterest(),
                "accruedInterestField", formField("Accrued Interest ($)")
                        .withInput(numberInput("accruedInterest", ticket.getAccruedInterest())
                                .withLoadIndicator("_fetching")
                                .withDisabled(true)),

                "tradeDate", formField("Trade Date")
                        .withInput(dateInput("tradeDate", ticket.getTradeDate())
                                .withDisabled(true)),

                "settleDate", formField("Settle Date")
                        .withInput(dateInput("settleDate", ticket.getSettleDate())));
    }

    private void bookTicket(TradeTicket ticket) {
        if (!isValid(ticket)) {
            throw new RuntimeException("Tried to book an invalid ticket.");
        }

        var trade = ticket.toTrade();
        if (trade.getId() != null) {
            tradeRepository.modifyTrade(trade);
        } else {
            tradeRepository.bookTrade(trade);
        }
    }

    private boolean isValid(TradeTicket ticket) {
        return validateCusip(ticket.getCusip()) == null && validateQuantity(ticket.getQuantity()) == null;
    }

    private String validateCusip(String cusip) {
        if(cusip == null || cusip.isBlank()) {
            return "This field is required";
        }

        if(!this.bondRepository.isValidCusip(cusip)) {
            return "The CUSIP is invalid - please specify a known CUSIP";
        }

        return null;
    }

    private String validateQuantity(BigDecimal quantity) {
        if(quantity == null) {
            return "This field is required";
        }

        if(quantity.compareTo(BigDecimal.ZERO) <= 0) {
            return "Quantity has to be > 0";
        }

        return null;
    }
}