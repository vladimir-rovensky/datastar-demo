package com.bookie.screens;

import com.bookie.components.DataGrid;
import com.bookie.components.Popup;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeRepository;
import com.bookie.infra.*;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static com.bookie.components.DataGrid.column;
import static com.bookie.infra.Format.usd;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class TradesScreen extends BaseScreen {

    private final TradeRepository tradeRepository;
    private final TradeTicketPopup tradeTicketPopup;

    private List<Trade> trades;
    private final Runnable unsubscribeFromTradeBooked;
    private final Runnable unsubscribeFromTradeModified;
    private final Runnable unsubscribeFromTradeDeleted;

    public TradesScreen(TradeRepository tradeRepository, TradeTicketPopup tradeTicketPopup,
                        SessionRegistry sessionRegistry, MessageBus messageBus) {
        super("Trades", sessionRegistry);

        this.tradeRepository = tradeRepository;
        this.tradeTicketPopup = tradeTicketPopup;

        this.unsubscribeFromTradeBooked = messageBus.subscribe(TradeBookedEvent.class, this::onTradeBooked);
        this.unsubscribeFromTradeModified = messageBus.subscribe(TradeModifiedEvent.class, this::onTradeModified);
        this.unsubscribeFromTradeDeleted = messageBus.subscribe(TradeDeletedEvent.class, this::onTradeDeleted);
    }

    @Override
    public void dispose() {
        this.unsubscribeFromTradeBooked.run();
        this.unsubscribeFromTradeModified.run();
        this.unsubscribeFromTradeDeleted.run();
    }

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .GET("", _ -> sessionRegistry
                        .createSession(TradesScreen.class)
                        .getScreen(TradesScreen.class)
                        .initialRender())
                .POST("modify/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .openModifyTicket(request))
                .GET("delete/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .openDeleteConfirmation(request))
                .POST("delete/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .deleteTradeById(request))
                .build();
    }

    public ServerResponse initialRender() {
        this.trades = tradeRepository.getAllTrades();

        return Response.html(render());
    }

    public ServerResponse openDeleteConfirmation(ServerRequest request) {
        var tradeId = Long.parseLong(request.pathVariable("id"));
        var content = Popup.popup()
                .withStyle(Popup.Style.WARNING)
                .withTitle("Cancel Trade")
                .withContent(html("<p>Are you sure you want to cancel trade ${tradeId}?</p>", "tradeId", tradeId))
                .withActions(html("""
                        <button class="btn-primary" data-on:click="@post('/trades/delete/${tradeId}')">Confirm</button>
                        <button data-on:click="@post('/tradeTicket/cancel')">Cancel</button>
                        """, "tradeId", tradeId))
                .render();
        return Popup.open(content);
    }

    public ServerResponse deleteTradeById(ServerRequest request) {
        var tradeId = Long.parseLong(request.pathVariable("id"));
        tradeRepository.deleteTrade(tradeId);
        return Popup.close();
    }

    public ServerResponse openModifyTicket(ServerRequest request) {
        var tradeID = Long.parseLong(request.pathVariable("id"));
        var trade = tradeRepository.findById(tradeID);
        return Popup.open(tradeTicketPopup.render(trade));
    }

    @Override
    protected EscapedHtml getContent() {
        var grid = DataGrid.withColumns(
                        column("", this::getCancelTradeButton),
                        column("ID", Trade::getId),
                        column("CUSIP", Trade::getCusip),
                        column("Book", Trade::getBook),
                        column("Type", Trade::getDirection),
                        column("Counterparty", Trade::getCounterparty),
                        column("Quantity", t -> usd(t.getQuantity())),
                        column("Accrued Interest", t -> usd(t.getAccruedInterest())),
                        column("Trade Date", Trade::getTradeDate),
                        column("Settle Date", Trade::getSettleDate))
                .onRowDoubleClick(t -> "@post('/trades/modify/" + t.getId() + "')")
                .withRows(this.trades.reversed())
                .render();

        return html("""
                    <div id="trades-screen" class="trades-screen">
                    ${styles}
                    ${grid}
                    </div>
                """,
                "styles", getStyles(),
                "grid", grid);
    }

    private EscapedHtml getStyles() {
        return html("""
                    <div id="trades-screen" class="trades-screen">
                    <style>
                        @scope {
                            td:first-child,
                            th:first-child {
                                width: 1px;
                                white-space: nowrap;
                                padding-right: 8px;
                                border-right: 1px solid #1e3d5c;
                            }
                        }
                    </style>
                """);
    }

    private EscapedHtml getCancelTradeButton(Trade t) {
        return html("""
                <button class="btn-no-bg" data-on:click="@get('/trades/delete/${tradeID}')" data-tooltip='Cancel Trade'>✕</button>
                """, "tradeID", t.getId());
    }

    private void onTradeBooked(TradeBookedEvent event) {
        this.trades.add(event.getTrade());
        reRender();
    }

    private void onTradeModified(TradeModifiedEvent event) {
        this.trades.replaceAll(t -> t.getId().equals(event.getTrade().getId()) ? event.getTrade() : t);
        reRender();
    }

    private void onTradeDeleted(TradeDeletedEvent event) {
        this.trades.removeIf(t -> t.getId().equals(event.getTradeId()));
        reRender();
    }

}