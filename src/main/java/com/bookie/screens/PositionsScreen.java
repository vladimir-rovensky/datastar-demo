package com.bookie.screens;

import com.bookie.components.CommonColumns;
import com.bookie.components.DataGrid;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.domain.entity.Position;
import com.bookie.domain.service.PositionService;
import com.bookie.infra.*;
import com.bookie.infra.events.BondSavedEvent;
import com.bookie.infra.events.PositionChangedEvent;
import com.bookie.infra.events.PositionsLoadedEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.bookie.components.DataGrid.column;
import static com.bookie.components.Link.link;
import com.bookie.infra.Format;
import static com.bookie.infra.Response.connectUpdates;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class PositionsScreen extends BaseScreen {

    private List<Position> positions;
    private final Map<String, Bond> bondByCusip = new HashMap<>();
    private final BondRepository bondRepository;

    private final List<Runnable> eventSubscriptions = new ArrayList<>();
    private final DataGrid<Position> positionGrid;

    public static final String RoutePrefix = "/positions";

    public PositionsScreen(PositionService positionService, BondRepository bondRepository, EventBus eventBus) {
        super("Positions");

        this.bondRepository = bondRepository;

        this.positionGrid = DataGrid.withColumns(
                        column("CUSIP", Position::getCusip)
                                .withRenderer(p -> link("securities/" + p.getCusip() + "/general", p.getCusip(), getRouteInfo().tabId().localID())),
                        column("Book", Position::getBook),
                        column("Description", p -> getBond(p.getCusip()).map(Bond::getDescription).orElse("")),
                        column("Current Position", Position::getCurrentPosition)
                                .withFormat(Format::usd),
                        column("Settled Position", Position::getSettledPosition)
                                .withFormat(Format::usd),
                        column("Last Activity", Position::getLastActivity)
                                .withFormat(Format::dateTime))
                .columns(CommonColumns.bondColumns(p -> getBond(p.getCusip())))
                .withRowID(p -> p.getCusip() + "-" + p.getBook())
                .withStripedRows()
                .withEndpoint("/positions/grid")
                .withColumnPicker()
                .withUpdateChannel(this::getChannel);

        this.eventSubscriptions.add(eventBus.subscribe(PositionsLoadedEvent.class, this::onPositionsLoaded));
        this.eventSubscriptions.add(eventBus.subscribe(PositionChangedEvent.class, this::onPositionChanged));
        this.eventSubscriptions.add(eventBus.subscribe(BondSavedEvent.class, this::onBondSaved));

        this.positions = new ArrayList<>(positionService.getPositions());
        loadBondsFor(this.positions);
    }

    public DataGrid<Position> getPositionGrid() {
        return this.positionGrid;
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
                .nest(RequestPredicates.path("/grid"), b -> DataGrid.setupRoutes(b, r -> sessionRegistry.getScreen(r, PositionsScreen.class).positionGrid))
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
        var grid = this.positionGrid.withRows(this.positions).render();

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
        loadBondsFor(this.positions);
        this.triggerUpdate();
    }

    private synchronized void onPositionChanged(PositionChangedEvent event) {
        Position changedPosition = event.position();
        this.positions.removeIf(p -> p.getKey().equals(changedPosition.getKey()));
        this.positions.add(changedPosition);
        this.positions.sort(Comparator.comparing(Position::getLastActivity).reversed());
        loadBondsFor(List.of(changedPosition));
        this.triggerUpdate();
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

    private void loadBondsFor(List<Position> positionList) {
        var missingCusips = new HashSet<String>();
        positionList.forEach(p -> missingCusips.add(p.getCusip()));
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
