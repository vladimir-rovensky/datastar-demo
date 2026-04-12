package com.bookie.screens.securities;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.SessionRegistry;
import com.bookie.screens.BaseScreen;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.bookie.components.Link.link;
import static com.bookie.infra.Response.connectUpdates;
import static com.bookie.infra.TemplatingEngine.html;

@Configuration
public class SecuritiesScreen extends BaseScreen {

    public static final String RoutePrefix = "/securities";
    public static final String NO_CUSIP = "nocusip";

    private final BondRepository bondRepository;

    private Bond currentBond;
    private Bond editingBond;
    private BondSection currentSection;

    private boolean isEditing() { return editingBond != null; }

    private Bond getActiveBond() { return isEditing() ? editingBond : currentBond; }

    public SecuritiesScreen(BondRepository bondRepository) {
        super("Securities");
        this.bondRepository = bondRepository;
        this.currentSection = BondSection.GENERAL;
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
                .POST("callSchedule/{rowID}/{field}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleCallScheduleUpdate(request))
                .POST("putSchedule/{rowID}/{field}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handlePutScheduleUpdate(request))
                .POST("sinkingFundSchedule/{rowID}/{field}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleSinkingFundScheduleUpdate(request))
                .POST("input/{field}", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).handleInput(request))
                .build();
    }

    public synchronized ServerResponse initialRender(ServerRequest request) {
        var cusip = request.pathVariable("cusip");
        var section = request.pathVariable("section");

        if(!Objects.equals(cusip, getCurrentCusip())) {
            currentBond = bondRepository.findBondByCusip(cusip);
            editingBond = null;
        }

        currentSection = BondSection.fromPath(section);

        updateRouteInfo(getRouteInfo().withActiveCusip(getCurrentCusip()).withActiveSection(currentSection.getPath()));

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

        return html("""
                <div id="securities-screen" class="fill-height">
                    ${styles}
                    ${secondaryToolbar}
                    ${body}
                </div>
                """,
                "styles", getStyles(),
                "secondaryToolbar", secondaryToolbar,
                "body", body);
    }

    private EscapedHtml renderSection() {
        return switch (currentSection) {
            case GENERAL -> GeneralSection.render(getActiveBond(), isEditing());
            case INCOME -> IncomeSection.render(getActiveBond(), isEditing());
            case REDEMPTION -> RedemptionSection.render(getActiveBond(), isEditing());
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

        return html("""
                <button class="btn-primary" data-on:click="@post('/securities/save')">Save</button>
                <button data-on:click="@post('/securities/cancel')">Cancel</button>
                """);
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

    public synchronized ServerResponse handleInput(ServerRequest request) throws Exception {
        var field = request.pathVariable("field");
        var bond = request.body(Bond.class);

        var bondField = Bond.class.getDeclaredField(field);
        bondField.setAccessible(true);
        bondField.set(editingBond, bondField.get(bond));

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
        var rowID = request.pathVariable("rowID");
        var field = request.pathVariable("field");
        var incomingEntry = request.body(Bond.CallEntry.class);

        var matchingEntry = editingBond.getCallSchedule().stream()
                .filter(e -> Objects.equals(e.getId(), rowID))
                .findFirst();

        if (matchingEntry.isPresent()) {
            var entryField = Bond.CallEntry.class.getDeclaredField(field);
            entryField.setAccessible(true);
            entryField.set(matchingEntry.get(), entryField.get(incomingEntry));
        }

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handlePutScheduleUpdate(ServerRequest request) throws Exception {
        var rowID = request.pathVariable("rowID");
        var field = request.pathVariable("field");
        var incomingEntry = request.body(Bond.PutEntry.class);

        var matchingEntry = editingBond.getPutSchedule().stream()
                .filter(e -> Objects.equals(e.getId(), rowID))
                .findFirst();

        if (matchingEntry.isPresent()) {
            var entryField = Bond.PutEntry.class.getDeclaredField(field);
            entryField.setAccessible(true);
            entryField.set(matchingEntry.get(), entryField.get(incomingEntry));
        }

        triggerUpdate();

        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse handleSinkingFundScheduleUpdate(ServerRequest request) throws Exception {
        var rowID = request.pathVariable("rowID");
        var field = request.pathVariable("field");
        var incomingEntry = request.body(Bond.SinkingFundEntry.class);

        var matchingEntry = editingBond.getSinkingFundSchedule().stream()
                .filter(e -> Objects.equals(e.getId(), rowID))
                .findFirst();

        if (matchingEntry.isPresent()) {
            var entryField = Bond.SinkingFundEntry.class.getDeclaredField(field);
            entryField.setAccessible(true);
            entryField.set(matchingEntry.get(), entryField.get(incomingEntry));
        }

        triggerUpdate();

        return ServerResponse.ok().build();
    }
}
