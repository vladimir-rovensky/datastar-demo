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
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.List;

import static com.bookie.components.DataGrid.column;
import static com.bookie.infra.Format.usd;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class PositionsScreen extends BaseScreen {

    private final TradeRepository tradeRepository;
    private final PositionService positionService;

    private List<Trade> trades;
    private List<Position> positions;

    private final Runnable unsubscribeFromTradeBooked;
    private final Runnable unsubscribeFromTradeModified;
    private final Runnable unsubscribeFromTradeDeleted;

    public PositionsScreen(TradeRepository tradeRepository, PositionService positionService,
                           SessionRegistry sessionRegistry, MessageBus messageBus) {
        super("Positions", sessionRegistry);

        this.tradeRepository = tradeRepository;
        this.positionService = positionService;
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
                        .createSession(PositionsScreen.class)
                        .getScreen(PositionsScreen.class)
                        .initialRender())
                .build();
    }

    public ServerResponse initialRender() {
        this.trades = tradeRepository.getAllTrades();
        this.positions = positionService.compute(this.trades);
        return Response.html(render());
    }

    @Override
    protected EscapedHtml getContent() {
        var grid = DataGrid.withColumns(
                        column("CUSIP", Position::getCusip),
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

    private void onTradeBooked(TradeBookedEvent event) {
        this.trades.add(event.getTrade());
        this.positions = positionService.compute(this.trades);
        reRender();
    }

    private void onTradeModified(TradeModifiedEvent event) {
        this.trades.replaceAll(t -> t.getId().equals(event.getTrade().getId()) ? event.getTrade() : t);
        this.positions = positionService.compute(this.trades);
        reRender();
    }

    private void onTradeDeleted(TradeDeletedEvent event) {
        this.trades.removeIf(t -> t.getId().equals(event.getTradeId()));
        this.positions = positionService.compute(this.trades);
        reRender();
    }
}
