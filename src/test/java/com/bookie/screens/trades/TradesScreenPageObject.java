package com.bookie.screens.trades;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.Trade;
import com.bookie.infra.DataGridHelper;
import com.bookie.infra.Format;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class TradesScreenPageObject {
    private final Page page;

    public TradesScreenPageObject(Page page) {
        this.page = page;
    }

    public DataGridHelper getGrid() {
        return DataGridHelper.find(getRoot());
    }

    public void verifyTradeDisplayed(Trade trade, Bond bond) {
        var grid = this.getGrid();
        var row = grid.getRowByCellValue("ID", trade.getId().toString());

        row.getCell("ID").verifyText(trade.getId().toString());
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
