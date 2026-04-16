package com.bookie.screens.securities;

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
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bookie.components.Link.link;
import static com.bookie.components.Loader.loader;
import static com.bookie.components.Notification.notification;
import static com.bookie.components.Notification.warning;
import static com.bookie.infra.Response.connectUpdates;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class SecuritiesScreen extends BaseScreen {

    public static final String RoutePrefix = "/securities";
    public static final String NO_CUSIP = "nocusip";

    private final BondRepository bondRepository;
    private final List<Runnable> eventSubscriptions = new ArrayList<>();

    private Bond currentBond;
    private Bond editingBond;
    private BondSection currentSection;

    private boolean isLoading = false;
    private boolean isModifiedBySomeoneElse = false;

    private boolean isEditing() { return editingBond != null; }

    private Bond getActiveBond() { return isEditing() ? editingBond : currentBond; }

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

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .GET("", _ -> ServerResponse.temporaryRedirect(
                        URI.create(RoutePrefix + "/" + NO_CUSIP + "/" + BondSection.GENERAL.getPath())).build())
                .GET("/{cusip}/{section}", request -> sessionRegistry
                        .getOrCreateSession(SecuritiesScreen.class, request)
                        .getScreen(SecuritiesScreen.class)
                        .initialRender(request))
                .POST("updates", request -> connectUpdates(sessionRegistry, SecuritiesScreen.class, request))
                .POST("edit", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).startEdit())
                .POST("save", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).saveEdit())
                .POST("cancel", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).cancelEdit())
                .POST("resetSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleResetScheduleUpdate(request))
                .PUT("resetSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addResetEntry())
                .DELETE("resetSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deleteResetEntry(request))
                .POST("callSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleCallScheduleUpdate(request))
                .PUT("callSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addCallEntry())
                .DELETE("callSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deleteCallEntry(request))
                .POST("putSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handlePutScheduleUpdate(request))
                .PUT("putSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addPutEntry())
                .DELETE("putSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deletePutEntry(request))
                .POST("sinkingFundSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleSinkingFundScheduleUpdate(request))
                .PUT("sinkingFundSchedule", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).addSinkingFundEntry())
                .DELETE("sinkingFundSchedule/{id}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).deleteSinkingFundEntry(request))
                .POST("input/{field}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleInput(request))
                .build();
    }

    public synchronized ServerResponse initialRender(ServerRequest request) {
        var cusip = request.pathVariable("cusip");
        var section = request.pathVariable("section");

        if(!Objects.equals(cusip, getCurrentCusip())) {
            currentBond = null;
            editingBond = null;
            if (NO_CUSIP.equals(cusip)) {
                isLoading = false;
            } else {
                isLoading = true;
                var targetCusip = cusip;
                Util.startAsync(() -> finishLoading(bondRepository.findBondByCusip(targetCusip)));
            }
        }

        currentSection = BondSection.fromPath(section);

        updateRouteInfo(getRouteInfo()
                .withActiveCusip(cusip)
                .withActiveSection(currentSection.getPath()));

        return handleInitialRender(request, this::render);
    }

    private String getCurrentCusip() {
        return currentBond != null ? currentBond.getCusip() : NO_CUSIP;
    }

    @Override
    protected String getUpdateURL() {
        return RoutePrefix + "/updates";
    }

    @Override
    protected synchronized EscapedHtml getContent() {
        var secondaryToolbar = renderSecondaryToolbar();
        var body = currentBond == null
                ? html("""
                        <p class="centered-message">Please load a CUSIP in the top-left.</p>
                        """)
                : renderSection();

        var refreshLink = link("securities/" + getCurrentCusip() + "/" + currentSection.getPath(), "refresh");
        var topBar = isModifiedBySomeoneElse
                ? notification(html("This bond was modified by someone else. Please ${refreshLink} the page.", "refreshLink", refreshLink)).withStyle(warning).render()
                : secondaryToolbar;

        return html("""
                <div id="securities-screen" class="fill-height">
                    ${styles}
                    ${topBar}
                    ${body}
                    ${loader}
                </div>
                """,
                "styles", getStyles(),
                "topBar", topBar,
                "body", body,
                "loader", loader(isLoading, "Loading Security..."));
    }

    private EscapedHtml renderSection() {
        return switch (currentSection) {
            case GENERAL -> GeneralSection.render(getActiveBond(), isEditing(), bondRepository);
            case INCOME -> IncomeSection.render(getActiveBond(), isEditing(), bondRepository);
            case REDEMPTION -> RedemptionSection.render(getActiveBond(), isEditing(), bondRepository);
        };
    }

    private EscapedHtml renderSecondaryToolbar() {
        var tabId = getRouteInfo().tabId().localID();
        var currentCusip = getCurrentCusip();
        var cusipValue = NO_CUSIP.equals(currentCusip) ? "" : currentCusip;
        var generalLink = link("securities/" + currentCusip + "/" + BondSection.GENERAL.getPath(), BondSection.GENERAL.getLabel(), tabId).withActive(currentSection == BondSection.GENERAL);
        var incomeLink = link("securities/" + currentCusip + "/" + BondSection.INCOME.getPath(), BondSection.INCOME.getLabel(), tabId).withActive(currentSection == BondSection.INCOME);
        var redemptionLink = link("securities/" + currentCusip + "/" + BondSection.REDEMPTION.getPath(), BondSection.REDEMPTION.getLabel(), tabId).withActive(currentSection == BondSection.REDEMPTION);
        var navigateToCusip = "if($cusipLookup.trim()) window.location.href='/securities/'+$cusipLookup.trim()+'/" + currentSection.getPath() + "?tabID=" + tabId + "'";
        var editActions = renderEditActions();

        return html("""
                <div class="toolbar secondary" data-signals="{cusipLookup: '${cusipValue}'}">
                    <div class="cusip-lookup">
                        <input type="text" data-bind="cusipLookup" placeholder="CUSIP" data-on:keydown.enter="${navigateToCusip}">
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

        if (!isEditing()) {
            return html("""
                    <button data-on:click="@post('/securities/edit')">Edit</button>
                    """);
        }

        var saveDisabled = !bondRepository.isValid(editingBond);
        return html("""
                <button class="btn-primary" ${saveDisabled} data-on:click="@post('/securities/save')">Save</button>
                <button data-on:click="@post('/securities/cancel')">Cancel</button>
                """,
                "saveDisabled", saveDisabled ? html("disabled data-tooltip=\"Please fix any invalid values before saving.\"") : EscapedHtml.blank());
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

                        .bond-general {
                            padding: var(--sp-lg);
                            align-content: start;
                        }

                        .bond-income {
                            padding: var(--sp-lg);
                            display: flex;
                            flex-direction: column;
                            gap: var(--sp-lg);
                        }

                        .bond-income .form-fields { align-content: start; }

                        .cusip-display {
                            padding: var(--sp-xs) var(--sp-sm);
                            color: var(--clr-text-faint);
                            border: 1px solid var(--clr-border-dim);
                            background-color: var(--clr-bg);
                        }

                        .bond-redemption {
                            padding: var(--sp-lg);
                            display: flex;
                            flex-direction: column;
                            gap: var(--sp-lg);
                        }

                        .redemption-panel {
                            display: flex;
                            flex-direction: column;
                            gap: var(--sp-sm);
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

    public synchronized ServerResponse saveEdit() {
        if (!bondRepository.isValid(editingBond)) {
            throw new RuntimeException("Tried to save an invalid bond.");
        }
        bondRepository.saveBond(editingBond);
        currentBond = editingBond;
        editingBond = null;
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse cancelEdit() {
        editingBond = null;
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    private synchronized void finishLoading(Bond bond) {
        if (bond != null && !bond.getCusip().equals(getRouteInfo().activeCusip())) {
            return;
        }
        currentBond = bond;
        isLoading = false;
        isModifiedBySomeoneElse = false;
        triggerUpdate();
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
        bondField.set(editingBond, bondField.get(bond));

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addResetEntry() {
        this.editingBond.getResetSchedule().add(bondRepository.createResetEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deleteResetEntry(ServerRequest request) {
        var id = request.pathVariable("id");
        this.editingBond.setResetSchedule(
                this.editingBond.getResetSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addCallEntry() {
        this.editingBond.getCallSchedule().add(bondRepository.createCallEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deleteCallEntry(ServerRequest request) {
        var id = request.pathVariable("id");
        this.editingBond.setCallSchedule(
                this.editingBond.getCallSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addPutEntry() {
        this.editingBond.getPutSchedule().add(bondRepository.createPutEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deletePutEntry(ServerRequest request) {
        var id = request.pathVariable("id");
        this.editingBond.setPutSchedule(
                this.editingBond.getPutSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse addSinkingFundEntry() {
        this.editingBond.getSinkingFundSchedule().add(bondRepository.createSinkingFundEntry());
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse deleteSinkingFundEntry(ServerRequest request) {
        var id = request.pathVariable("id");
        this.editingBond.setSinkingFundSchedule(
                this.editingBond.getSinkingFundSchedule().stream()
                        .filter(e -> !e.getId().equals(id))
                        .collect(Collectors.toList()));
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handleResetScheduleUpdate(ServerRequest request) throws Exception {
        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.ResetEntry>>>() {});
        Map<String, Bond.ResetEntry> incomingEntries = incomingData.get("resetSchedule");

        this.editingBond.setResetSchedule(
                this.editingBond.getResetSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));


        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handleCallScheduleUpdate(ServerRequest request) throws Exception {
        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.CallEntry>>>() {});
        Map<String, Bond.CallEntry> incomingEntries = incomingData.get("callSchedule");

        this.editingBond.setCallSchedule(
                this.editingBond.getCallSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handlePutScheduleUpdate(ServerRequest request) throws Exception {
        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.PutEntry>>>() {});
        Map<String, Bond.PutEntry> incomingEntries = incomingData.get("putSchedule");

        this.editingBond.setPutSchedule(
                this.editingBond.getPutSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handleSinkingFundScheduleUpdate(ServerRequest request) throws Exception {
        var incomingData = request.body(new ParameterizedTypeReference<Map<String, Map<String, Bond.SinkingFundEntry>>>() {});
        Map<String, Bond.SinkingFundEntry> incomingEntries = incomingData.get("sinkingFundSchedule");

        this.editingBond.setSinkingFundSchedule(
                this.editingBond.getSinkingFundSchedule().stream()
                        .map(e -> incomingEntries.getOrDefault(e.getId(), e))
                        .collect(Collectors.toList()));

        triggerUpdate();

        return ServerResponse.ok().build();
    }
}
