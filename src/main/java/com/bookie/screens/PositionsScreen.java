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
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.math.BigDecimal;
import java.util.ArrayList;
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

    private List<Position> positions = new ArrayList<>();
    private final Map<String, Bond> bondByCusip = new HashMap<>();
    private final BondRepository bondRepository;

    private final List<Runnable> eventSubscriptions = new ArrayList<>();
    private final DataGrid<Position> positionGrid;

    public static final String RoutePrefix = "/positions";

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

    public PositionsScreen(PositionService positionService, BondRepository bondRepository, EventBus eventBus) {
        super("Positions");

        this.bondRepository = bondRepository;

        this.positionGrid = DataGrid.withColumns(
                        column("CUSIP", Position::getCusip)
                                .withRenderer(p -> link("securities/" + p.getCusip() + "/general", p.getCusip())),
                        column("Book", Position::getBook),
                        column("Description", p -> getBond(p.getCusip()).map(Bond::getDescription).orElse("")),
                        column("Current Position", Position::getCurrentPosition)
                                .withFormat(Format::usd),
                        column("Settled Position", Position::getSettledPosition)
                                .withFormat(Format::usd),
                        column("Last Activity", Position::getLastActivity)
                                .withFormat(Format::dateTime))
                .columns(CommonColumns.bondColumns(p -> getBond(p.getCusip())))
                .withDefaultSort("last_activity", DataGrid.SortDirection.Descending)
                .withRowID(p -> p.getCusip() + "-" + p.getBook())
                .withStripedRows()
                .withEndpoint("/positions/grid")
                .withColumnPicker()
                .filterable()
                .withUpdateChannel(this::getChannel);

        this.eventSubscriptions.add(eventBus.subscribeBatched()
                .on(PositionChangedEvent.class, this::onPositionChanged)
                .on(BondSavedEvent.class, this::onBondSaved)
                .afterBatchProcessed(() -> {
                    this.loadBondsFor(positions);
                    this.triggerUpdate();
                })
                .subscribe());

        this.positions = loadPositions(positionService);
        loadBondsFor(this.positions);
    }

    private synchronized ArrayList<Position> loadPositions(PositionService positionService) {
        return new ArrayList<>(positionService.getPositions());
    }

    public DataGrid<Position> getPositionGrid() {
        return this.positionGrid;
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
        positionGrid.handleInitialRender();
        return handleInitialRender(request, this::render);
    }

    @Override
    protected synchronized EscapedHtml getContent() {
        var grid = this.positionGrid.withRows(this.positions).render();

        return html("""
                    <div id="positions-screen" role="main" class="fill-height">
                    ${grid}
                    </div>
                """,
                "grid", grid);
    }

    private synchronized void onPositionChanged(PositionChangedEvent event) {
        Position changedPosition = event.position();
        this.positions.removeIf(p -> p.getKey().equals(changedPosition.getKey()));
        this.positions.add(changedPosition);
        animatePositionCellsIfChanged(event);
    }

    private void animatePositionCellsIfChanged(PositionChangedEvent event) {
        animateCellIfChanged(event.position(), "Current Position", event.previousPosition().getCurrentPosition(), event.position().getCurrentPosition());
        animateCellIfChanged(event.position(), "Settled Position", event.previousPosition().getSettledPosition(), event.position().getSettledPosition());
    }

    private void animateCellIfChanged(Position row, String header, BigDecimal previousValue, BigDecimal currentValue) {
        if (currentValue.compareTo(previousValue) == 0) {
            return;
        }

        var increased = currentValue.compareTo(previousValue) > 0;
        var keyframes = increased ? "[{color:'var(--clr-success-border)'},{color:'inherit'}]" : "[{color:'var(--clr-error)'},{color:'inherit'}]";
        this.positionGrid.animateCell(row, header, keyframes, 750);
    }

    private synchronized void onBondSaved(BondSavedEvent event) {
        var bond = event.getBond();
        if (bondByCusip.containsKey(bond.getCusip())) {
            bondByCusip.put(bond.getCusip(), bond);
        }
    }

    private Optional<Bond> getBond(String cusip) {
        return Optional.ofNullable(bondByCusip.get(cusip));
    }

    private synchronized void loadBondsFor(List<Position> positionList) {
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
