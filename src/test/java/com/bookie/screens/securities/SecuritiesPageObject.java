package com.bookie.screens.securities;

import com.bookie.infra.*;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import org.springframework.lang.NonNull;

import java.time.LocalDate;

public class SecuritiesPageObject {
    private final Page page;
    private final HealthIndicatorHelper healthIndicator;

    public SecuritiesPageObject(Page page, HealthIndicatorHelper healthIndicator) {
        this.page = page;
        this.healthIndicator = healthIndicator;
    }

    public SecuritiesPageObject loadCusip(String cusip) {
        page.getByPlaceholder("CUSIP").fill(cusip);
        ButtonHelper.getByLabel(getToolbar(), "Load").click();
        healthIndicator.waitUntilHealthy();
        return this;
    }

    public SecuritiesPageObject switchToEditMode() {
        ButtonHelper.getByLabel(getToolbar(), "Edit").click();
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

    public DataGridHelper getSinkScheduleGrid() {
        var sinkSchedulePanel = page.getByRole(AriaRole.HEADING)
                .filter(new Locator.FilterOptions().setHasText("Sink Schedule"))
                .locator("..");
        return DataGridHelper.find(sinkSchedulePanel);
    }

    public void addSinkRow() {
        getSinkScheduleGrid().addRow();
    }

    public void save() {
        ButtonHelper.getByLabel(getToolbar(), "Save").click();
    }

    private Locator getToolbar() {
        return page.getByRole(AriaRole.TOOLBAR, new Page.GetByRoleOptions().setName("Subsections")).first();
    }

    @NonNull
    private FormHelper getFormHelper() {
        return new FormHelper(page.locator(":scope"));
    }
}
