package com.bookie.screens;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Position;
import com.bookie.domain.entity.Trade;
import com.bookie.domain.entity.TradeRepository;
import com.bookie.domain.service.PositionService;
import com.bookie.infra.*;
import com.bookie.infra.events.TradeBookedEvent;
import com.bookie.infra.events.TradeDeletedEvent;
import com.bookie.infra.events.TradeModifiedEvent;
import com.bookie.infra.events.TradesLoadedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.ArrayList;
import java.util.List;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.Link.link;
import static com.bookie.infra.Format.usd;
import static com.bookie.infra.Response.connectUpdates;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class PositionsScreen extends BaseScreen {

    private final PositionService positionService;

    private List<Trade> trades;
    private List<Position> positions;

    private final List<Runnable> eventSubscriptions = new ArrayList<>();

    public static final String RoutePrefix = "/positions";

    public PositionsScreen(TradeRepository tradeRepository, PositionService positionService,
                           EventBus eventBus) {
        super("Positions");

        this.positionService = positionService;
        this.eventSubscriptions.add(eventBus.subscribe(TradesLoadedEvent.class, this::onTradesLoaded));
        this.eventSubscriptions.add(eventBus.subscribe(TradeBookedEvent.class, this::onTradeBooked));
        this.eventSubscriptions.add(eventBus.subscribe(TradeModifiedEvent.class, this::onTradeModified));
        this.eventSubscriptions.add(eventBus.subscribe(TradeDeletedEvent.class, this::onTradeDeleted));

        this.trades = tradeRepository.getAllTrades();
        this.positions = positionService.compute(this.trades);
    }

    @Override
    public void dispose() {
        this.eventSubscriptions.reversed().forEach(Runnable::run);
    }

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .GET("", request -> sessionRegistry
                        .getOrCreateSession(PositionsScreen.class, request)
                        .getScreen(PositionsScreen.class)
                        .initialRender(request))
                .POST("updates", request -> connectUpdates(sessionRegistry, PositionsScreen.class, request))
                .build();
    }

    @Override
    protected String getUpdateURL() {
        return RoutePrefix + "/updates";
    }

    public synchronized ServerResponse initialRender(ServerRequest request) {
        return handleInitialRender(request, this::render);
    }

    @Override
    protected synchronized EscapedHtml getContent() {
        var grid = DataGrid.withColumns(
                        column("CUSIP", p -> link("securities/" + p.getCusip() + "/general", p.getCusip(), getTabID().localID()).render()),
                        column("Book", Position::getBook),
                        column("Current Position", p -> usd(p.getCurrentPosition())),
                        column("Settled Position", p -> usd(p.getSettledPosition())),
                        column("Last Activity", Position::getLastActivity))
                .withRows(this.positions)
                .render();

        return html("""
                    <div id="positions-screen" class="positions-screen">
                    ${grid}
                    </div>
                """,
                "grid", grid);
    }

    private synchronized void onTradesLoaded(TradesLoadedEvent event) {
        this.trades = new ArrayList<>(event.getTrades());
        this.positions = positionService.compute(this.trades);
        this.triggerUpdate();
    }

    private synchronized void onTradeBooked(TradeBookedEvent event) {
        this.trades.add(event.getTrade());
        this.positions = positionService.compute(this.trades);
        this.triggerUpdate();
    }

    private synchronized void onTradeModified(TradeModifiedEvent event) {
        this.trades.replaceAll(t -> t.getId().equals(event.getTrade().getId()) ? event.getTrade() : t);
        this.positions = positionService.compute(this.trades);
        this.triggerUpdate();
    }

    private synchronized void onTradeDeleted(TradeDeletedEvent event) {
        this.trades.removeIf(t -> t.getId().equals(event.getTradeId()));
        this.positions = positionService.compute(this.trades);
        this.triggerUpdate();
    }
}
