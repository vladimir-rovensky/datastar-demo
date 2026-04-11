package com.bookie.screens.securities;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.infra.EscapedHtml;

import static com.bookie.components.DataGrid.column;
import static com.bookie.infra.TemplatingEngine.html;

public class RedemptionSection {

    public static EscapedHtml render(Bond bond) {
        var callSchedule = bond.getCallSchedule();
        var callTable = (callSchedule == null || callSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No call schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Call Date", Bond.CallEntry::callDate),
                        column("Call Price", Bond.CallEntry::callPrice))
                        .withRows(callSchedule)
                        .render();

        var putSchedule = bond.getPutSchedule();
        var putTable = (putSchedule == null || putSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No put schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Put Date", Bond.PutEntry::putDate),
                        column("Put Price", Bond.PutEntry::putPrice))
                        .withRows(putSchedule)
                        .render();

        var sinkingFundSchedule = bond.getSinkingFundSchedule();
        var sinkingFundTable = (sinkingFundSchedule == null || sinkingFundSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No sinking fund schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Sink Date", Bond.SinkingFundEntry::sinkDate),
                        column("Amount", Bond.SinkingFundEntry::amount))
                        .withRows(sinkingFundSchedule)
                        .render();

        return html("""
                <div class="bond-redemption">
                    <h3>Call Schedule</h3>
                    ${callTable}
                    <h3>Put Schedule</h3>
                    ${putTable}
                    <h3>Sinking Fund</h3>
                    ${sinkingFundTable}
                </div>
                """,
                "callTable", callTable,
                "putTable", putTable,
                "sinkingFundTable", sinkingFundTable);
    }
}
