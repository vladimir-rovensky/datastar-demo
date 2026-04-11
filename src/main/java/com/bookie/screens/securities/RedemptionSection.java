package com.bookie.screens.securities;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.infra.EscapedHtml;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.infra.TemplatingEngine.html;

public class RedemptionSection {

    public static EscapedHtml render(Bond bond, boolean editing) {
        var disabled = !editing;

        var callSchedule = bond.getCallSchedule();
        var callTable = (callSchedule == null || callSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No call schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Call Date", Bond.CallEntry::getCallDate)
                                .withRenderer(r -> dateInput("callDate", r.getCallDate())
                                        .withDisabled(disabled)
                                        .noBind()),
                        column("Call Price", Bond.CallEntry::getCallPrice)
                                .withRenderer(r -> numberInput("callPrice", r.getCallPrice())
                                        .withDisabled(disabled)
                                        .noBind()))
                        .withRows(callSchedule)
                        .withRowID(Bond.CallEntry::getId)
                        .withRowAttrs(r -> html("""
                                data-on:change="@post('/securities/callSchedule/${rowID}/' + evt.target.name, {payload: {[evt.target.name]: evt.target.value}})"
                                """, "rowID", r.getId()))
                        .render();

        var putSchedule = bond.getPutSchedule();
        var putTable = (putSchedule == null || putSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No put schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Put Date", Bond.PutEntry::getPutDate)
                                .withRenderer(r -> dateInput("putDate", r.getPutDate())
                                        .withDisabled(disabled)
                                        .noBind()),
                        column("Put Price", Bond.PutEntry::getPutPrice)
                                .withRenderer(r -> numberInput("putPrice", r.getPutPrice())
                                        .withDisabled(disabled)
                                        .noBind()))
                        .withRows(putSchedule)
                        .withRowID(Bond.PutEntry::getId)
                        .withRowAttrs(r -> html("""
                                data-on:change="@post('/securities/putSchedule/${rowID}/' + evt.target.name, {payload: {[evt.target.name]: evt.target.value}})"
                                """, "rowID", r.getId()))
                        .render();

        var sinkingFundSchedule = bond.getSinkingFundSchedule();
        var sinkingFundTable = (sinkingFundSchedule == null || sinkingFundSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No sinking fund schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Sink Date", Bond.SinkingFundEntry::getSinkDate)
                                .withRenderer(r -> dateInput("sinkDate", r.getSinkDate())
                                        .withDisabled(disabled)
                                        .noBind()),
                        column("Amount", Bond.SinkingFundEntry::getAmount)
                                .withRenderer(r -> numberInput("amount", r.getAmount())
                                        .withDisabled(disabled)
                                        .noBind()))
                        .withRows(sinkingFundSchedule)
                        .withRowID(Bond.SinkingFundEntry::getId)
                        .withRowAttrs(r -> html("""
                                data-on:change="@post('/securities/sinkingFundSchedule/${rowID}/' + evt.target.name, {payload: {[evt.target.name]: evt.target.value}})"
                                """, "rowID", r.getId()))
                        .render();

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
                        .data-grid { width: 500px; }
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
