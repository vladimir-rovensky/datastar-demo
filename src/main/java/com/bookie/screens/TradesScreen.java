package com.bookie.screens;

import com.bookie.components.CommonColumns;
import com.bookie.components.DataGrid;
import com.bookie.components.Popup;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeRepository;
import com.bookie.infra.*;
import com.bookie.infra.events.BondSavedEvent;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.Link.link;
import com.bookie.infra.Format;
import static com.bookie.infra.HtmlExtensions.X;
import static com.bookie.infra.Response.connectUpdates;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class TradesScreen extends BaseScreen {

    private final TradeRepository tradeRepository;
    private final TradeTicketPopup tradeTicketPopup;
    private final BondRepository bondRepository;

    private List<Trade> trades = new ArrayList<>();
    private final Map<String, Bond> bondByCusip = new HashMap<>();
    private final List<Runnable> eventSubscriptions = new ArrayList<>();
    private final DataGrid<Trade> tradeGrid;

    public static final String RoutePrefix = "/trades";

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .GET("", request -> sessionRegistry
                        .getOrCreateSession(TradesScreen.class, request)
                        .getScreen(TradesScreen.class)
                        .initialRender(request))
                .POST("updates", request -> connectUpdates(sessionRegistry, TradesScreen.class, request))
                .POST("modify/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .openModifyTicket(request))
                .GET("delete/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .openDeleteConfirmation(request))
                .POST("delete/{id}", request -> sessionRegistry
                        .getScreen(request, TradesScreen.class)
                        .deleteTradeById(request))
                .nest(RequestPredicates.path("/grid"), b -> DataGrid.setupRoutes(b, r -> sessionRegistry.getScreen(r, TradesScreen.class).tradeGrid))
                .build();
    }

    public TradesScreen(TradeRepository tradeRepository, TradeTicketPopup tradeTicketPopup,
                        BondRepository bondRepository, EventBus eventBus) {
        super("Trades");

        this.tradeRepository = tradeRepository;
        this.tradeTicketPopup = tradeTicketPopup;
        this.bondRepository = bondRepository;

        this.eventSubscriptions.add(eventBus.subscribeBatched()
                .on(TradeBookedEvent.class, this::onTradeBooked)
                .on(TradeModifiedEvent.class, this::onTradeModified)
                .on(TradeDeletedEvent.class, this::onTradeDeleted)
                .afterBatchProcessed(this::triggerUpdate)
                .subscribe());
        this.eventSubscriptions.add(eventBus.subscribe(BondSavedEvent.class, this::onBondSaved));

        loadTrades(tradeRepository);

        this.tradeGrid = DataGrid.withColumns(
                        column("ID", Trade::getId),
                        column("CUSIP", Trade::getCusip)
                                .withRenderer(t -> link("securities/" + t.getCusip() + "/general", t.getCusip())),
                        column("Description", t -> getBond(t.getCusip()).map(Bond::getDescription).orElse("")),
                        column("Book", Trade::getBook),
                        column("Type", Trade::getDirection),
                        column("Counterparty", Trade::getCounterparty),
                        column("Quantity", Trade::getQuantity)
                                .withFormat(Format::usd),
                        column("Accrued Interest", Trade::getAccruedInterest)
                                .withFormat(Format::usd),
                        column("Trade Date", Trade::getTradeDate),
                        column("Settle Date", Trade::getSettleDate))
                .columns(CommonColumns.bondColumns(t -> getBond(t.getCusip())))
                .withDefaultSort("id", DataGrid.SortDirection.Descending)
                .withRowID(r -> "trade-" + r.getId())
                .onRowDoubleClick(t -> X.post("/trades/modify/" + t.getId()).render())
                .onDeleteRow(t -> X.get("/trades/delete/" + t.getId()).render())
                .withDeleteRowTooltip("Cancel Trade")
                .withStripedRows()
                .withEndpoint("/trades/grid")
                .withColumnPicker()
                .filterable()
                .withUpdateChannel(this::getChannel);

        loadBondsFor(this.trades);
    }

    private synchronized void loadTrades(TradeRepository tradeRepository) {
        this.trades = new ArrayList<>(tradeRepository.getAllTrades());
    }

    @Override
    public void dispose() {
        this.eventSubscriptions.reversed().forEach(Runnable::run);
    }

    @Override
    protected String getUpdateURL() {
        return RoutePrefix + "/updates";
    }

    public synchronized ServerResponse initialRender(ServerRequest request) {
        tradeGrid.handleInitialRender();
        return handleInitialRender(request, this::render);
    }

    public ServerResponse openDeleteConfirmation(ServerRequest request) {
        var tradeId = Long.parseLong(request.pathVariable("id"));
        var content = Popup.popup()
                .withStyle(Popup.Style.WARNING)
                .withTitle("Cancel Trade")
                .withContent(html("""
                        <p>Are you sure you want to cancel trade ${tradeId}?</p>
                        """, "tradeId", tradeId))
                .withActions(html("""
                        <button class="btn-primary" data-on:click="${confirmAction}">Confirm</button>
                        <button data-on:click="${cancelAction}">Cancel</button>
                        """,
                        "confirmAction", X.post("/trades/delete/" + tradeId),
                        "cancelAction", X.post("/tradeTicket/cancel")))
                .render();
        return Popup.open(content);
    }

    public ServerResponse deleteTradeById(ServerRequest request) {
        var tradeId = Long.parseLong(request.pathVariable("id"));

        if(!tradeRepository.deleteTrade(tradeId)) {
            throw new UserFacingException("Cancelling the trade would result in a short position");
        }

        return Popup.close();
    }

    public ServerResponse openModifyTicket(ServerRequest request) {
        var tradeID = Long.parseLong(request.pathVariable("id"));
        var trade = tradeRepository.findById(tradeID);
        return Popup.open(tradeTicketPopup.render(trade));
    }

    public DataGrid<Trade> getTradeGrid() {
        return this.tradeGrid;
    }

    @Override
    protected synchronized EscapedHtml getContent() {
        var grid = this.tradeGrid.withRows(this.trades).render();

        return html("""
                    <div id="trades-screen" role="main" class="fill-height">
                    ${grid}
                    </div>
                """,
                "grid", grid);
    }

    private synchronized void onTradeBooked(TradeBookedEvent event) {
        Trade bookedTrade = event.getTrade();
        this.trades.removeIf(t -> t.getId().equals(bookedTrade.getId()));
        this.trades.add(bookedTrade);
        loadBondsFor(List.of(bookedTrade));
    }

    private synchronized void onTradeModified(TradeModifiedEvent event) {
        this.trades.replaceAll(t -> t.getId().equals(event.updatedTrade().getId()) ? event.updatedTrade() : t);
        loadBondsFor(List.of(event.updatedTrade()));
    }

    private synchronized void onTradeDeleted(TradeDeletedEvent event) {
        this.trades.removeIf(t -> t.getId().equals(event.deletedTrade().getId()));
    }

    private synchronized void onBondSaved(BondSavedEvent event) {
        var bond = event.getBond();
        if (bondByCusip.containsKey(bond.getCusip())) {
            bondByCusip.put(bond.getCusip(), bond);
            triggerUpdate();
        }
    }

    private Optional<Bond> getBond(String cusip) {
        return Optional.ofNullable(bondByCusip.get(cusip));
    }

    private void loadBondsFor(List<Trade> tradeList) {
        var missingCusips = new HashSet<String>();
        tradeList.forEach(t -> missingCusips.add(t.getCusip()));
        missingCusips.removeAll(bondByCusip.keySet());
        if (missingCusips.isEmpty()) {
            return;
        }
        Util.startAsync(() -> {
            var loaded = bondRepository.findBondsByCusips(missingCusips);
            synchronized (this) {
                bondByCusip.putAll(loaded);
                triggerUpdate();
            }
        });
    }

}
