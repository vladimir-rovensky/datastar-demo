package com.bookie.screens.securities;

import com.bookie.components.Notification;
import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.infra.EventBus;
import com.bookie.infra.Util;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.SessionRegistry;
import com.bookie.infra.events.BondSavedEvent;
import com.bookie.screens.BaseScreen;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.bookie.components.Link.link;
import static com.bookie.components.Loader.loader;
import static com.bookie.components.Notification.notification;
import static com.bookie.components.Notification.warning;
import static com.bookie.infra.HtmlExtensions.X;
import static com.bookie.infra.Response.connectUpdates;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class SecuritiesScreen extends BaseScreen {

    public static final String RoutePrefix = "/security";
    public static final String NO_CUSIP = "nocusip";

    private final BondRepository bondRepository;
    private final List<Runnable> eventSubscriptions = new ArrayList<>();

    private Bond currentBond;
    private Bond editingBond;
    private BondSection currentSection;

    private final AtomicInteger loadingCount = new AtomicInteger(0);
    private boolean isModifiedBySomeoneElse = false;

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .route(RequestPredicates.GET("").and(RequestPredicates.param("updates", _ -> true)),
                        request -> connectUpdates(sessionRegistry, SecuritiesScreen.class, request))
                .GET("", _ -> ServerResponse.temporaryRedirect(
                        URI.create(RoutePrefix + "/" + NO_CUSIP)).build())
                .GET("/{cusip}", request -> sessionRegistry
                        .getOrCreateSession(SecuritiesScreen.class, request)
                        .getScreen(SecuritiesScreen.class)
                        .initialRender(request))
                .POST("/{cusip}/edit", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).startEdit())
                .PUT("/{cusip}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).saveEdit(request))
                .DELETE("/{cusip}/edit", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).cancelEdit())
                .PUT("", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).lookupCusip(request))
                .PUT("/{cusip}/edit/resetSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleResetScheduleUpdate(request))
                .POST("/{cusip}/edit/resetSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addResetEntry(request))
                .DELETE("/{cusip}/edit/resetSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deleteResetEntry(request))
                .PUT("/{cusip}/edit/callSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleCallScheduleUpdate(request))
                .POST("/{cusip}/edit/callSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addCallEntry(request))
                .DELETE("/{cusip}/edit/callSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deleteCallEntry(request))
                .PUT("/{cusip}/edit/putSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handlePutScheduleUpdate(request))
                .POST("/{cusip}/edit/putSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addPutEntry(request))
                .DELETE("/{cusip}/edit/putSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deletePutEntry(request))
                .PUT("/{cusip}/edit/sinkingFundSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleSinkingFundScheduleUpdate(request))
                .POST("/{cusip}/edit/sinkingFundSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addSinkingFundEntry(request))
                .DELETE("/{cusip}/edit/sinkingFundSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deleteSinkingFundEntry(request))
                .PUT("/{cusip}/edit/{field}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleInput(request))
                .build();
    }

    private boolean isEditing() { return editingBond != null; }

    private Bond getActiveBond() { return isEditing() ? editingBond : currentBond; }

    private Bond getEditingBond(ServerRequest request) {
        var requestCusip = request.pathVariable("cusip");
        if(!Objects.equals(requestCusip, editingBond != null ? editingBond.getCusip() : null)) {
            throw new RuntimeException("Invalid / Stale update request.");
        }

        return editingBond;
    }

    public SecuritiesScreen(BondRepository bondRepository, EventBus eventBus) {
        super("Securities");
        this.bondRepository = bondRepository;
        this.currentSection = BondSection.GENERAL;
        this.eventSubscriptions.add(eventBus.subscribe(BondSavedEvent.class, this::onBondSaved));
    }

    @Override
    public void dispose() {
        this.eventSubscriptions.reversed().forEach(Runnable::run);
    }

    public synchronized ServerResponse initialRender(ServerRequest request) {
        var cusip = request.pathVariable("cusip");
        var section = request.param("section").orElse(BondSection.GENERAL.getPath());

        currentSection = BondSection.fromPath(section);

        startLoadingCusip(cusip, false);

        return handleInitialRender(request, this::render);
    }

    public synchronized ServerResponse lookupCusip(ServerRequest request) throws Exception {
        var body = request.body(new ParameterizedTypeReference<Map<String, String>>() {});
        var cusip = body.get("cusipLookup").trim();

        startLoadingCusip(cusip, true);

        triggerUpdate();
        return ServerResponse.ok().build();
    }

    private void startLoadingCusip(String cusip, boolean updateURL) {
        if (Objects.equals(cusip, getRouteInfo().activeCusip())) {
            return;
        }

        updateRouteInfo(getRouteInfo()
                .withActiveCusip(cusip)
                .withActiveSection(currentSection.getPath()));

        if (NO_CUSIP.equals(cusip)) {
            currentBond = null;
            editingBond = null;
        } else {
            loadCusip(cusip, updateURL);
        }
    }

    private String getCurrentCusip() {
        return currentBond != null ? currentBond.getCusip() : NO_CUSIP;
    }

    @Override
    protected String getUpdateURL() {
        return RoutePrefix + "?updates";
    }

    @Override
    protected synchronized EscapedHtml getContent() {
        var secondaryToolbar = renderSecondaryToolbar();
        var body = currentBond == null
                ? html("""
                        <p class="centered-message">Please load a CUSIP in the top-left.</p>
                        """)
                : renderSection();

        var topBar = isModifiedBySomeoneElse
                ? notification(html("This bond was modified by someone else. Please refresh the page.")).withStyle(warning).inline().render()
                : secondaryToolbar;

        return html("""
                <div id="securities-screen" role="main" class="fill-height">
                    ${styles}
                    ${topBar}
                    ${body}
                    ${loader}
                </div>
                """,
                "styles", getStyles(),
                "topBar", topBar,
                "body", body,
                "loader", loader(loadingCount.get() != 0, "Loading Security..."));
    }

    private EscapedHtml renderSection() {
        return switch (currentSection) {
            case GENERAL -> GeneralSection.render(getActiveBond(), isEditing(), bondRepository);
            case INCOME -> IncomeSection.render(getActiveBond(), isEditing(), bondRepository);
            case REDEMPTION -> RedemptionSection.render(getActiveBond(), isEditing(), bondRepository);
        };
    }

    private EscapedHtml renderSecondaryToolbar() {
        var currentCusip = getCurrentCusip();
        var cusipValue = NO_CUSIP.equals(currentCusip) ? "" : currentCusip;
        var generalLink = link("security/" + currentCusip + "?section=" + BondSection.GENERAL.getPath(), BondSection.GENERAL.getLabel()).withActive(currentSection == BondSection.GENERAL);
        var incomeLink = link("security/" + currentCusip + "?section=" + BondSection.INCOME.getPath(), BondSection.INCOME.getLabel()).withActive(currentSection == BondSection.INCOME);
        var redemptionLink = link("security/" + currentCusip + "?section=" + BondSection.REDEMPTION.getPath(), BondSection.REDEMPTION.getLabel()).withActive(currentSection == BondSection.REDEMPTION);

        var loadCusip = X.put("/security").withIncludeSignals("cusipLookup");

        var navigateToCusip = html("""
                if($cusipLookup.trim()) { ${loadCusip}; }
                """, "loadCusip", loadCusip);

        var editActions = renderEditActions();

        return html("""
                <div class="toolbar secondary" role="toolbar" aria-label="Subsections" data-signals="{cusipLookup: '${cusipValue}'}">
                    <div class="cusip-lookup">
                        <input type="text" data-bind="cusipLookup" placeholder="CUSIP">
                        <button data-on:click="${navigateToCusip}">Load</button>
                    </div>
                    <div class="toolbar-separator"></div>
                    <nav class="screen-nav">
                        ${generalLink}
                        ${incomeLink}
                        ${redemptionLink}
                    </nav>
                    <div class="edit-actions">${editActions}</div>
                </div>
                """,
                "cusipValue", cusipValue,
                "navigateToCusip", navigateToCusip,
                "generalLink", generalLink,
                "incomeLink", incomeLink,
                "redemptionLink", redemptionLink,
                "editActions", editActions);
    }

    private EscapedHtml renderEditActions() {
        if (currentBond == null) {
            return EscapedHtml.blank();
        }

        var cusip = getCurrentCusip();

        if (!isEditing()) {
            return html("""
                    <button data-on:click="${editAction}">Edit</button>
                    """,
                    "editAction", X.post("/security/" + cusip + "/edit"));
        }

        var saveDisabled = !bondRepository.isValid(editingBond);
        return html("""
                <button class="btn-primary" ${saveDisabled} data-on:click="${saveAction}">Save</button>
                <button data-on:click="${cancelAction}">Cancel</button>
                """,
                "saveAction", X.put("/security/" + cusip),
                "cancelAction", X.delete("/security/" + cusip + "/edit"),
                "saveDisabled", saveDisabled ? html("""
                        disabled data-tooltip="Please fix any invalid values before saving." """) : EscapedHtml.blank());
    }

    private EscapedHtml getStyles() {
        return html("""
                <style>
                    @scope {
                        .cusip-lookup {
                            display: flex;
                            gap: var(--sp-xs);
                        }

                        .cusip-lookup input { width: 120px; }

                        .edit-actions { margin-left: auto; }

                        .form-fields {
                            width: 800px;
                            column-gap: var(--sp-lg);
                        }

                    }
                </style>
                """);
    }

    public synchronized ServerResponse startEdit() {
        editingBond = currentBond.clone();
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse saveEdit(ServerRequest request) {
        Bond editingBond = getEditingBond(request);
        bondRepository.saveBond(editingBond);
        currentBond = editingBond;
        this.editingBond = null;
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse cancelEdit() {
        editingBond = null;
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    private synchronized void loadCusip(String cusip, boolean updateURL) {
        loadingCount.incrementAndGet();

        Util.startAsync(() -> {
            var bond = bondRepository.findBondByCusip(cusip);
            finishLoadingCusip(bond, updateURL);
        });
    }

    private synchronized void finishLoadingCusip(Bond bond, boolean updateURL) {
        try {
            if (bond != null && !bond.getCusip().equals(getRouteInfo().activeCusip())) {
                return;
            }

            if (bond != null) {
                currentBond = bond;
                editingBond = null;
                isModifiedBySomeoneElse = false;

                if (updateURL) {
                    getChannel().updateURL(RoutePrefix + "/" + bond.getCusip() + "?section=" + currentSection.getPath());
                }
            } else {
                getChannel().updateFragment(notification(html("This CUSIP does not exist in the system."))
                        .withStyle(Notification.NotificationStyle.ERROR)
                        .render());
            }
        } finally {
            loadingCount.decrementAndGet();
            triggerUpdate();
        }
    }

    private synchronized void onBondSaved(BondSavedEvent event) {
        if (currentBond == null) {
            return;
        }

        if (Objects.equals(event.getBond().getCusip(), currentBond.getCusip())
                && event.getBond().getVersion() != currentBond.getVersion()) {
            isModifiedBySomeoneElse = true;
            triggerUpdate();
        }
    }

    public synchronized ServerResponse handleInput(ServerRequest request) throws Exception {
        var field = request.pathVariable("field");
        var bond = request.body(Bond.class);

        var bondField = Bond.class.getDeclaredField(field);
        bondField.setAccessible(true);
        bondField.set(getEditingBond(request), bondField.get(bond));

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addResetEntry(ServerRequest request) {
        getEditingBond(request).getResetSchedule().add(bondRepository.createResetEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deleteResetEntry(ServerRequest request) {
        var id = request.pathVariable("id");
        var editingBond = getEditingBond(request);
        editingBond.setResetSchedule(
                editingBond.getResetSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addCallEntry(ServerRequest request) {
        getEditingBond(request).getCallSchedule().add(bondRepository.createCallEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deleteCallEntry(ServerRequest request) {
        var id = request.pathVariable("id");
        var editingBond = getEditingBond(request);
        editingBond.setCallSchedule(
                editingBond.getCallSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addPutEntry(ServerRequest request) {
        getEditingBond(request).getPutSchedule().add(bondRepository.createPutEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deletePutEntry(ServerRequest request) {
        var editingBond = getEditingBond(request);
        var id = request.pathVariable("id");
        editingBond.setPutSchedule(
                editingBond.getPutSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addSinkingFundEntry(ServerRequest request) {
        getEditingBond(request).getSinkingFundSchedule().add(bondRepository.createSinkingFundEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deleteSinkingFundEntry(ServerRequest request) {
        var editingBond = getEditingBond(request);

        var id = request.pathVariable("id");
        editingBond.setSinkingFundSchedule(
                editingBond.getSinkingFundSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handleResetScheduleUpdate(ServerRequest request) throws Exception {

        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.ResetEntry>>>() {});
        Map<String, Bond.ResetEntry> incomingEntries = incomingData.get("resetSchedule");

        var editingBond = getEditingBond(request);

        editingBond.setResetSchedule(
                editingBond.getResetSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));


        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handleCallScheduleUpdate(ServerRequest request) throws Exception {

        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.CallEntry>>>() {});
        Map<String, Bond.CallEntry> incomingEntries = incomingData.get("callSchedule");

        var editingBond = getEditingBond(request);

        editingBond.setCallSchedule(
                editingBond.getCallSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handlePutScheduleUpdate(ServerRequest request) throws Exception {

        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.PutEntry>>>() {});
        Map<String, Bond.PutEntry> incomingEntries = incomingData.get("putSchedule");

        var editingBond = getEditingBond(request);

        editingBond.setPutSchedule(
                editingBond.getPutSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handleSinkingFundScheduleUpdate(ServerRequest request) throws Exception {

        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.SinkingFundEntry>>>() {});
        Map<String, Bond.SinkingFundEntry> incomingEntries = incomingData.get("sinkingFundSchedule");

        var editingBond = getEditingBond(request);

        editingBond.setSinkingFundSchedule(
                editingBond.getSinkingFundSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));

        triggerUpdate();

        return ServerResponse.ok().build();
    }

}
