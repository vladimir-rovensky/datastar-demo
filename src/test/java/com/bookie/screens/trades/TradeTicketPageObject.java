package com.bookie.screens.trades;

import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import com.bookie.infra.*;
import com.microsoft.playwright.Locator;
import org.springframework.lang.NonNull;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class TradeTicketPageObject {

    private final Locator root;

    public TradeTicketPageObject(Locator root) {
        this.root = root;
    }

    public TradeTicketPageObject enterTrade(Trade trade) {
        getCusip().setValue(trade.getCusip());
        getBook().setValue(trade.getBook());
        getCounterparty().setValue(trade.getCounterparty());
        getQuantity().setValue(trade.getQuantity());
        getSettleDate().setValue(trade.getSettleDate());
        return this;
    }

    public void confirm(Trade trade) {
        if(trade.getId() == null) {
            confirmNewTrade(trade.getDirection());
        } else {
            confirmModifyTrade();
        }
    }

    private TextInputHelper getCusip() {
        return getForm().getTextField("CUSIP");
    }

    private SelectInputHelper getBook() {
        return getForm().getSelectField("Book");
    }

    private DateInputHelper getSettleDate() {
        return getForm().getDateField("Settle Date");
    }

    private SelectInputHelper getCounterparty() {
        return getForm().getSelectField("Counterparty");
    }

    public NumberInputHelper getQuantity() {
        return getForm().getNumericField("Quantity ($)");
    }

    @NonNull
    private FormHelper getForm() {
        return new FormHelper(this.root);
    }

    private void confirmModifyTrade() {
        ButtonHelper.getByLabel(this.root, "OK").click();
    }

    private void confirmNewTrade(TradeDirection direction) {
        ButtonHelper.getByLabel(this.root, direction.getLabel()).click();
    }

    public void cancel() {
        ButtonHelper.getByLabel(this.root, "Cancel").click();
    }

    public TradeTicketPageObject waitForAccruedInterest() {
        assertThat(this.root.getByText("Accrued Interest ($)").locator("number-input")).not().hasClass("loading");
        return this;
    }
}
