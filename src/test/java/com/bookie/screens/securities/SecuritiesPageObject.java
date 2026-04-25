package com.bookie.screens.securities;

import com.bookie.infra.*;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.springframework.lang.NonNull;

import java.time.LocalDate;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class SecuritiesPageObject {
    private final Page page;
    private final HealthIndicatorHelper healthIndicator;

    public SecuritiesPageObject(Page page, HealthIndicatorHelper healthIndicator) {
        this.page = page;
        this.healthIndicator = healthIndicator;
    }

    public SecuritiesPageObject loadCusip(String cusip) {
        startLoadingSecurity(cusip);
        waitForSecurityToLoad();
        healthIndicator.waitUntilHealthy();
        return this;
    }

    public void startLoadingSecurity(String cusip) {
        page.getByPlaceholder("CUSIP").fill(cusip);
        ButtonHelper.getByLabel(getToolbar(), "Load").click();
    }

    private void waitForSecurityToLoad() {
        assertThat(page.getByRole(AriaRole.MAIN)).not().containsText("Please load a CUSIP in the top-left.");
    }

    public SecuritiesPageObject switchToEditMode() {
        getEditButton().click();
        return this;
    }

    public SecuritiesPageObject switchToIncome() {
        LinkHelper.getByLabel(getToolbar(), "Income").click();
        healthIndicator.waitUntilHealthy();
        return this;
    }

    public SecuritiesPageObject switchToRedemption() {
        LinkHelper.getByLabel(getToolbar(), "Redemption").click();
        healthIndicator.waitUntilHealthy();
        return this;
    }

    public TextInputHelper getDescription() {
        return getFormHelper().getTextField("Description");
    }

    public NumberInputHelper getIssueSize() {
        return getFormHelper().getNumericField("Issue Size");
    }

    public NumberInputHelper getOutstandingAmount() {
        return getFormHelper().getNumericField("Outstanding Amount");
    }

    public SecuritiesPageObject addSinkRow(LocalDate date, Number value) {
        this.addSinkRow();
        var sinkRow = this.getSinkScheduleGrid().getFirstRow();
        sinkRow.getCell("Sink Date").getDateInput().setValue(date);
        sinkRow.getCell("Amount").getNumberInput().setValue(value);
        return this;
    }

    public SecuritiesPageObject addResetRow(LocalDate date, Number rate) {
        getResetScheduleGrid().addRow();

        var resetRow = this.getResetScheduleGrid().getLastRow();
        resetRow.getCell("Reset Date").getDateInput().setValue(date);
        resetRow.getCell("New Rate").getNumberInput().setValue(rate);
        return this;
    }

    public SecuritiesPageObject deleteResetRow(int index) {
        getResetScheduleGrid().getRow(index).delete();
        return this;
    }

    public DataGridHelper getSinkScheduleGrid() {
        return getGridByLabel("Sink Schedule");
    }

    public DataGridHelper getResetScheduleGrid() {
        return getGridByLabel("Reset Schedule");
    }

    private DataGridHelper getGridByLabel(String labe) {
        return DataGridHelper.find(page.getByRole(AriaRole.HEADING)
                .filter(new Locator.FilterOptions().setHasText(labe))
                .locator(".."));
    }

    public void addSinkRow() {
        getSinkScheduleGrid().addRow();
    }

    public void save() {
        getSaveButton().click();
        getEditButton().verifyButtonExists();
    }

    private ButtonHelper getSaveButton() {
        return ButtonHelper.getByLabel(getToolbar(), "Save");
    }

    private ButtonHelper getEditButton() {
        return ButtonHelper.getByLabel(getToolbar(), "Edit");
    }

    private Locator getToolbar() {
        return page.getByRole(AriaRole.TOOLBAR, new Page.GetByRoleOptions().setName("Subsections")).first();
    }

    @NonNull
    private FormHelper getFormHelper() {
        return new FormHelper(page.locator(":scope"));
    }
}
