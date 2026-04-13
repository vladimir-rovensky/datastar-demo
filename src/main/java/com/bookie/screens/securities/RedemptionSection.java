package com.bookie.screens.securities;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.infra.EscapedHtml;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.infra.TemplatingEngine.html;

public class RedemptionSection {

    public static EscapedHtml render(Bond bond, boolean editing, BondRepository bondRepository) {
        var disabled = !editing;

        var callSchedule = bond.getCallSchedule();
        var callTable = html("""
                        <div data-on:change="@post('/securities/callSchedule', {filterSignals: {include: /callSchedule.*/}})" class="flex fill-height">
                            ${grid}
                        </div>
                        """, "grid",
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
                                .onDeleteRow(!disabled ? r -> html("@delete('/securities/callSchedule/${id}')", "id", r.getId()) : null)
                                .onAddRow(!disabled ? html("@put('/securities/callSchedule')") : null)
                                .withNoRowsMessage("No Call Schedule")
                                .render());

        var putSchedule = bond.getPutSchedule();
        var putTable = html("""
                        <div data-on:change="@post('/securities/putSchedule', {filterSignals: {include: /putSchedule.*/}})" class="flex fill-height">
                            ${grid}
                        </div>
                        """, "grid",
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
                                .onDeleteRow(!disabled ? r -> html("@delete('/securities/putSchedule/${id}')", "id", r.getId()) : null)
                                .onAddRow(!disabled ? html("@put('/securities/putSchedule')") : null)
                                .withNoRowsMessage("No Put Schedule")
                                .render());

        var sinkingFundSchedule = bond.getSinkingFundSchedule();
        var sinkingFundTable = html("""
                        <div data-on:change="@post('/securities/sinkingFundSchedule', {filterSignals: {include: /sinkingFundSchedule.*/}})" class="flex fill-height">
                            ${grid}
                        </div>
                        """, "grid",
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
                                .onDeleteRow(!disabled ? r -> html("@delete('/securities/sinkingFundSchedule/${id}')", "id", r.getId()) : null)
                                .onAddRow(!disabled ? html("@put('/securities/sinkingFundSchedule')") : null)
                                .withNoRowsMessage("No Sinking Fund Schedule")
                                .render());

        return html("""
                <div class="bond-redemption fill-height">
                    <div class="redemption-panel fill-height">
                        <h3>Call Schedule</h3>
                        ${callTable}
                    </div>
                    <div class="redemption-panel fill-height">
                        <h3>Put Schedule</h3>
                        ${putTable}
                    </div>
                    <div class="redemption-panel fill-height">
                        <h3>Sinking Fund</h3>
                        ${sinkingFundTable}
                    </div>

                    <style>
                    @scope {
                        .data-grid { max-width: 500px; }
                        .data-grid-cell input { width: 100%; }
                    }
                    </style>
                </div>
                """,
                "callTable", callTable,
                "putTable", putTable,
                "sinkingFundTable", sinkingFundTable);
    }
}
