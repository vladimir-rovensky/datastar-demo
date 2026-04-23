package com.bookie.screens.trades;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeDirection;
import com.bookie.infra.ButtonHelper;
import com.bookie.infra.DataGridHelper;
import com.bookie.infra.Format;
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

    public TradesScreenPageObject bookTrade(Trade trade) {
        return bookTrade(trade, () -> {});
    }

    public TradesScreenPageObject bookTrade(Trade trade, Runnable beforeConfirm) {

        var ticket = openTradeTicket(trade.getDirection())
                .enterTrade(trade)
                .waitForAccruedInterest();

        beforeConfirm.run();

        ticket.confirm();

        return this;
    }

    public TradeTicketPageObject openTradeTicket(TradeDirection direction) {
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(direction == TradeDirection.BUY ? "B" : "S")).click();
        return getTradeTicket();
    }

    public void modifyTrade(Trade trade) {
        getGrid().getRowByCellValue("ID", trade.getId().toString()).dblclick();

        getTradeTicket().enterTrade(trade).confirm();
    }

    public TradeTicketPageObject getTradeTicket() {
        return new TradeTicketPageObject(getPopup());
    }

    public void cancelTrade(Long tradeId) {
        getGrid().getRowByCellValue("ID", tradeId.toString()).delete();

        ButtonHelper.getByLabel(getPopup(), "Confirm").click();
    }

    public void cancelAction() {
        ButtonHelper.getByLabel(getPopup(), "Cancel").click();
    }

    public void verifyNoTradesDisplayed() {
        getGrid().assertNoRows();
    }

    private Locator getPopup() {
        return page.getByRole(AriaRole.DIALOG);
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
