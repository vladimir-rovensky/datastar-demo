package com.bookie.screens;

import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Position;
import com.bookie.domain.service.PositionService;
import com.bookie.infra.*;
import com.bookie.infra.events.PositionChangedEvent;
import com.bookie.infra.events.PositionsLoadedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.Link.link;
import static com.bookie.infra.Format.usd;
import static com.bookie.infra.Response.connectUpdates;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class PositionsScreen extends BaseScreen {

    private List<Position> positions;

    private final List<Runnable> eventSubscriptions = new ArrayList<>();

    public static final String RoutePrefix = "/positions";

    public PositionsScreen(PositionService positionService, EventBus eventBus) {
        super("Positions");

        this.eventSubscriptions.add(eventBus.subscribe(PositionsLoadedEvent.class, this::onPositionsLoaded));
        this.eventSubscriptions.add(eventBus.subscribe(PositionChangedEvent.class, this::onPositionChanged));

        this.positions = positionService.getPositions();
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
                        column("CUSIP", p -> link("securities/" + p.getCusip() + "/general", p.getCusip(), getRouteInfo().tabId().localID()).render()),
                        column("Book", Position::getBook),
                        column("Current Position", p -> usd(p.getCurrentPosition())),
                        column("Settled Position", p -> usd(p.getSettledPosition())),
                        column("Last Activity", Position::getLastActivity))
                .withRows(this.positions)
                .withRowID(p -> p.getCusip() + "-" + p.getBook())
                .render();

        return html("""
                    <div id="positions-screen" class="positions-screen fill-height">
                    ${grid}
                    </div>
                """,
                "grid", grid);
    }

    private synchronized void onPositionsLoaded(PositionsLoadedEvent event) {
        this.positions = new ArrayList<>(event.positions());
        this.positions.sort(Comparator.comparing(Position::getLastActivity).reversed());
        this.triggerUpdate();
    }

    private synchronized void onPositionChanged(PositionChangedEvent event) {
        Position changedPosition = event.position();
        this.positions.removeIf(p -> p.getKey().equals(changedPosition.getKey()));
        this.positions.add(changedPosition);
        this.positions.sort(Comparator.comparing(Position::getLastActivity).reversed());
        this.triggerUpdate();
    }
}
