package com.bookie.screens.positions;

import com.bookie.domain.entity.Position;
import com.bookie.infra.DataGridHelper;
import com.bookie.infra.Format;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class PositionsScreenPageObject {
    private final Page page;

    public PositionsScreenPageObject(Page page) {
        this.page = page;
    }

    public DataGridHelper getGrid() {
        return DataGridHelper.find(page.getByRole(AriaRole.MAIN));
    }

    public void verifyPositionsDisplayed(Position... positions) {
        var grid = getGrid();
        for (Position position : positions) {
            var row = grid.getRowByCellValues("CUSIP", position.getCusip(), "Book", position.getBook());
            row.getCell("CUSIP").verifyText(position.getCusip());
            row.getCell("Book").verifyText(position.getBook());
            row.getCell("Current Position").verifyText(Format.usd(position.getCurrentPosition()));
            row.getCell("Settled Position").verifyText(Format.usd(position.getSettledPosition()));

            if (position.getLastActivity() != null) {
                row.getCell("Last Activity").verifyText(Format.dateTime(position.getLastActivity()));
            }
        }
    }
}
