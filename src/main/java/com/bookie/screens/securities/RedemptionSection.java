package com.bookie.screens.securities;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.infra.EscapedHtml;

import java.math.BigDecimal;
import java.time.LocalDate;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.infra.HtmlExtensions.X;
import static com.bookie.infra.TemplatingEngine.html;

public class RedemptionSection {

    public static EscapedHtml render(Bond bond, boolean editing, BondRepository bondRepository) {
        var disabled = !editing;

        var outstandingAmount = bond.getOutstandingAmount();

        var callSchedule = bond.getCallSchedule();
        var callTable = html("""
                        <div data-on:change="${callScheduleAction}" class="flex fill-height">
                            ${grid}
                        </div>
                        """,
                        "callScheduleAction", X.put("/security/" + bond.getCusip() + "/edit/callSchedule").withIncludeSignals("callSchedule.*"),
                        "grid",
                        DataGrid.withColumns(
                                column("Call Date", Bond.CallEntry::getCallDate)
                                        .withRenderer(r -> formField()
                                                .withInput(dateInput("callSchedule." + r.getId() + ".callDate", r.getCallDate()).withDisabled(disabled))
                                                .withError(bondRepository.validateCallScheduleDate(r.getCallDate(), callSchedule))
                                                .render()),
                                column("Call Price", Bond.CallEntry::getCallPrice)
                                        .withRenderer(r -> formField()
                                                .withInput(numberInput("callSchedule." + r.getId() + ".callPrice", r.getCallPrice()).withDisabled(disabled))
                                                .withError(bondRepository.validateCallSchedulePrice(r.getCallPrice()))
                                                .render()))
                                .withRows(callSchedule)
                                .withRowID(Bond.CallEntry::getId)
                                .withRowIDSignal(r -> "callSchedule." + r.getId() + ".id")
                                .onDeleteRow(!disabled ? r -> X.delete("/security/" + bond.getCusip() + "/edit/callSchedule/" + r.getId()).render() : null)
                                .onAddRow(!disabled ? X.post("/security/" + bond.getCusip() + "/edit/callSchedule").render() : null)
                                .withNoRowsMessage("No Call Schedule")
                                .render());

        var putSchedule = bond.getPutSchedule();
        var putTable = html("""
                        <div data-on:change="${putScheduleAction}" class="flex fill-height">
                            ${grid}
                        </div>
                        """,
                        "putScheduleAction", X.put("/security/" + bond.getCusip() + "/edit/putSchedule").withIncludeSignals("putSchedule.*"),
                        "grid",
                        DataGrid.withColumns(
                                column("Put Date", Bond.PutEntry::getPutDate)
                                        .withRenderer(r -> formField()
                                                .withInput(dateInput("putSchedule." + r.getId() + ".putDate", r.getPutDate()).withDisabled(disabled))
                                                .withError(bondRepository.validatePutScheduleDate(r.getPutDate(), putSchedule))
                                                .render()),
                                column("Put Price", Bond.PutEntry::getPutPrice)
                                        .withRenderer(r -> formField()
                                                .withInput(numberInput("putSchedule." + r.getId() + ".putPrice", r.getPutPrice()).withDisabled(disabled))
                                                .withError(bondRepository.validatePutSchedulePrice(r.getPutPrice()))
                                                .render()))
                                .withRows(putSchedule)
                                .withRowID(Bond.PutEntry::getId)
                                .withRowIDSignal(r -> "putSchedule." + r.getId() + ".id")
                                .onDeleteRow(!disabled ? r -> X.delete("/security/" + bond.getCusip() + "/edit/putSchedule/" + r.getId()).render() : null)
                                .onAddRow(!disabled ? X.post("/security/" + bond.getCusip() + "/edit/putSchedule").render() : null)
                                .withNoRowsMessage("No Put Schedule")
                                .render());

        var sinkingFundSchedule = bond.getSinkingFundSchedule();
        var sinkingFundTable = html("""
                        <div data-on:change="${sinkingFundScheduleAction}" class="flex fill-height">
                            ${grid}
                        </div>
                        """,
                        "sinkingFundScheduleAction", X.put("/security/" + bond.getCusip() + "/edit/sinkingFundSchedule").withIncludeSignals("sinkingFundSchedule.*"),
                        "grid",
                        DataGrid.withColumns(
                                column("Sink Date", Bond.SinkingFundEntry::getSinkDate)
                                        .withRenderer(r -> formField()
                                                .withInput(dateInput("sinkingFundSchedule." + r.getId() + ".sinkDate", r.getSinkDate()).withDisabled(disabled))
                                                .withError(bondRepository.validateSinkingFundScheduleDate(r.getSinkDate(), sinkingFundSchedule))
                                                .render()),
                                column("Amount", Bond.SinkingFundEntry::getAmount)
                                        .withRenderer(r -> formField()
                                                .withInput(numberInput("sinkingFundSchedule." + r.getId() + ".amount", r.getAmount()).withFormat("currency").withDisabled(disabled))
                                                .withError(bondRepository.validateSinkingFundScheduleAmount(r.getAmount()))
                                                .render()))
                                .withRows(sinkingFundSchedule)
                                .withRowID(Bond.SinkingFundEntry::getId)
                                .withRowIDSignal(r -> "sinkingFundSchedule." + r.getId() + ".id")
                                .onDeleteRow(!disabled ? r -> X.delete("/security/" + bond.getCusip() + "/edit/sinkingFundSchedule/" + r.getId()).render() : null)
                                .onAddRow(!disabled ? X.post("/security/" + bond.getCusip() + "/edit/sinkingFundSchedule").render() : null)
                                .withNoRowsMessage("No Sinking Fund Schedule")
                                .render());

        return html("""
                <div class="bond-redemption fill-height">
                    <div class="form-fields" data-on:change="${inputAction}">
                        ${issueSize}
                        ${outstandingAmount}
                    </div>
                    <div class="redemption-panel">
                        <h3>Sink Schedule</h3>
                        ${sinkingFundTable}
                    </div>
                    <div class="redemption-panel">
                        <h3>Call Schedule</h3>
                        ${callTable}
                    </div>
                    <div class="redemption-panel">
                        <h3>Put Schedule</h3>
                        ${putTable}
                    </div>

                    <style>
                    @scope {
                        :scope {
                            padding: var(--sp-lg);
                            display: grid;
                            grid-template-columns: 1fr 1fr 1fr;
                            grid-template-rows: auto 1fr;
                            gap: var(--sp-lg);
                        }

                        .form-fields {
                            grid-column: 1;
                            grid-row: 1;
                            width: auto;
                        }

                        .redemption-panel {
                            grid-row: 2;
                            display: flex;
                            flex-direction: column;
                            gap: var(--sp-sm);
                        }

                        .data-grid-cell input { width: 100%; }
                    }
                    </style>
                </div>
                """,
                "inputAction", X.put(html("'/security/" + bond.getCusip() + "/edit/' + evt.target.name"))
                        .withRequestCancellation(false)
                        .withIncludeSignals(html("new RegExp(evt.target.name)")),
                "issueSize", formField("Issue Size").withInput(numberInput("issueSize", bond.getIssueSize()).withFormat("currency").withDisabled(disabled))
                        .withError(bondRepository.validateIssueSize(bond.getIssueSize())),
                "outstandingAmount", formField("Outstanding Amount").withInput(numberInput("outstandingAmount", outstandingAmount).withFormat("currency").withDisabled(true)),
                "callTable", callTable,
                "putTable", putTable,
                "sinkingFundTable", sinkingFundTable);
    }

}
