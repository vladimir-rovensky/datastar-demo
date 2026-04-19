package com.bookie.screens.trades;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.Trade;
import com.bookie.infra.DataGridHelper;
import com.bookie.infra.Format;
import com.bookie.infra.FormHelper;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

import java.util.regex.Pattern;

public class TradesScreenPageObject {
    private final Page page;

    public TradesScreenPageObject(Page page) {
        this.page = page;
    }

    public DataGridHelper getGrid() {
        return DataGridHelper.find(getRoot());
    }

    public void bookBuyTrade(Trade trade) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("B")).click();

        FormHelper form = new FormHelper(page.getByRole(AriaRole.DIALOG));
        form.getTextField("CUSIP").setValue(trade.getCusip());
        form.getSelectField("Book").setValue(trade.getBook());
        form.getSelectField("Counterparty").setValue(trade.getCounterparty());
        form.getNumericField("Quantity ($)").setValue(trade.getQuantity());
        form.getDateField("Settle Date").setValue(trade.getSettleDate());

        page.getByRole(AriaRole.DIALOG)
            .getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(trade.getDirection().getLabel()))
            .click();
    }

    public void verifyTradeDisplayed(int rowIndex, Trade trade, Bond bond) {
        var grid = this.getGrid();
        var row = grid.getRow(rowIndex);

        if(trade.getId() != null) {
            row.getCell("ID").verifyText(trade.getId().toString());
        } else {
            row.getCell("ID").verifyText(Pattern.compile("[0-9]+"));
        }

        row.getCell("CUSIP").verifyText(trade.getCusip());
        row.getCell("DESCRIPTION").verifyText(bond.getDescription());
        row.getCell("BOOK").verifyText(trade.getBook());
        row.getCell("TYPE").verifyText(trade.getDirection().toString());
        row.getCell("COUNTERPARTY").verifyText(trade.getCounterparty());
        row.getCell("QUANTITY").verifyText(Format.usd(trade.getQuantity()));
        row.getCell("ACCRUED INTEREST").verifyText(Format.usd(trade.getAccruedInterest()));
        row.getCell("TRADE DATE").verifyText(Format.usDate(trade.getTradeDate()));
        row.getCell("SETTLE DATE").verifyText(Format.usDate(trade.getSettleDate()));
    }

    private Locator getRoot() {
        return page.getByRole(AriaRole.MAIN);
    }
}
