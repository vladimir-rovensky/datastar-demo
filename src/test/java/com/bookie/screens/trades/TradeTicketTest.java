package com.bookie.screens.trades;

import com.bookie.TestBase;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static com.bookie.infra.builders.BondBuilder.aBond;
import static com.bookie.infra.builders.TradeBuilder.aTrade;
import static org.hamcrest.MatcherAssert.assertThat;

public class TradeTicketTest extends TestBase {

    @Test
    public void disablesBookButtonWhenBooking() {
        givenExistingBonds(aBond("CSP1"));
        CompletableFuture<Void> bookSignal = new CompletableFuture<>();
        getTradeDAO().setBookAvailableSignal(bookSignal);

        var trades = page.switchToTrades();

        var ticket = trades.openTradeTicket(TradeDirection.BUY);

        Trade trade = aTrade("CSP1", TradeDirection.BUY, 10_000);

        ticket.enterTrade(trade);
        ticket.confirm();

        assertThat(ticket.getConfirmButton().isClickable(), Matchers.equalTo(false));
        bookSignal.complete(null);
    }

}
