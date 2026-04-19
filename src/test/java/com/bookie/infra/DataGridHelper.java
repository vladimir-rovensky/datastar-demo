package com.bookie.infra;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;

import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class DataGridHelper {
    private final Locator root;

    public DataGridHelper(Locator gridRoot) {
        this.root = gridRoot;
    }

    public static DataGridHelper find(Locator root) {
        return new DataGridHelper(root.getByRole(AriaRole.GRID));
    }

    public GridRowHelper getFirstRow() {
        return new GridRowHelper(this.root.getByRole(AriaRole.ROW).first(), this);
    }

    public GridRowHelper getSingleRow() {
        return new GridRowHelper(this.root.getByRole(AriaRole.ROW), this);
    }

    public GridRowHelper getRow(int index) {
        return new GridRowHelper(this.root.getByRole(AriaRole.ROW).nth(index), this);
    }

    public List<GridRowHelper> getRows() {
        return this.root.getByRole(AriaRole.ROW)
                .all()
                .stream()
                .map(r -> new GridRowHelper(r, this))
                .toList();
    }

    private int getColumnIndexByHeader(String header) {
        Locator columnHeader = this.root.getByRole(AriaRole.COLUMNHEADER, new Locator.GetByRoleOptions().setName(header));
        columnHeader.waitFor();

        return (int) this.root.getByRole(AriaRole.COLUMNHEADER).evaluateAll(
                "elements => elements.findIndex(el => el.innerText.trim() === '" + header + "')"
        );
    }

    public GridRowHelper getRowByCellValue(String header, String value) {
        var columnIndex = getColumnIndexByHeader(header);
        Locator rowLocator = this.root.getByRole(AriaRole.ROW);
        var row = rowLocator.filter(
                new Locator.FilterOptions().setHas(
                        rowLocator.page().getByRole(AriaRole.GRIDCELL).nth(columnIndex).getByText(value)));

        return new GridRowHelper(row, this);
    }

    public static class GridRowHelper {
        private final Locator root;
        private final DataGridHelper grid;

        public GridRowHelper(Locator root, DataGridHelper grid) {
            this.root = root;
            this.grid = grid;
        }

        public GridCellHelper getCell(String header) {
            return getCell(this.grid.getColumnIndexByHeader(header));
        }

        public GridCellHelper getCell(int index) {
            return new GridCellHelper(this.root.getByRole(AriaRole.GRIDCELL).nth(index));
        }

        public List<GridCellHelper> getCells() {
            return this.root.getByRole(AriaRole.GRIDCELL)
                    .all()
                    .stream()
                    .map(GridCellHelper::new)
                    .toList();
        }
    }

    public static class GridCellHelper {
        private final Locator root;

        public GridCellHelper(Locator root) {
            this.root = root;
        }

        public void verifyText(String text) {
            assertThat(this.root).containsText(text);
        }

        public void verifyText(Pattern regex) {
            assertThat(this.root).containsText(regex);
        }
    }
}
