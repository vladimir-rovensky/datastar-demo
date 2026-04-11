package com.bookie.screens.securities;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.SessionRegistry;
import com.bookie.screens.BaseScreen;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;

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
    private String currentCusip;
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
                .POST("save", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).saveEdit(request))
                .POST("cancel", request -> sessionRegistry.getScreen(request, SecuritiesScreen.class).cancelEdit())
                .build();
    }

    public synchronized ServerResponse initialRender(ServerRequest request) {
        var cusip = request.pathVariable("cusip");
        var section = request.pathVariable("section");
        var bond = bondRepository.findBondByCusip(cusip);

        currentSection = BondSection.fromPath(section);
        currentCusip = bond != null ? cusip : NO_CUSIP;
        currentBond = bond;

        return handleInitialRender(request, this::render);
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
                        <p class="securities-empty">Please load a CUSIP in the top-left.</p>
                        """)
                : renderSection();

        return html("""
                <div id="securities-screen">
                    ${secondaryToolbar}
                    ${body}
                </div>
                """,
                "secondaryToolbar", secondaryToolbar,
                "body", body);
    }

    private EscapedHtml renderSection() {
        return switch (currentSection) {
            case GENERAL -> GeneralSection.render(getActiveBond(), isEditing());
            case INCOME -> IncomeSection.render(getActiveBond(), isEditing());
            case REDEMPTION -> RedemptionSection.render(getActiveBond());
        };
    }

    private EscapedHtml renderSecondaryToolbar() {
        var tabId = getTabID().localID();
        var cusipValue = NO_CUSIP.equals(currentCusip) ? "" : currentCusip;
        var generalLink = link("securities/" + currentCusip + "/" + BondSection.GENERAL.getPath(), BondSection.GENERAL.getLabel(), tabId).withActive(currentSection == BondSection.GENERAL);
        var incomeLink = link("securities/" + currentCusip + "/" + BondSection.INCOME.getPath(), BondSection.INCOME.getLabel(), tabId).withActive(currentSection == BondSection.INCOME);
        var redemptionLink = link("securities/" + currentCusip + "/" + BondSection.REDEMPTION.getPath(), BondSection.REDEMPTION.getLabel(), tabId).withActive(currentSection == BondSection.REDEMPTION);
        var navigateToCusip = "if($cusipLookup.trim()) window.location.href='/securities/'+$cusipLookup.trim()+'/" + currentSection.getPath() + "?tabID=" + tabId + "'";
        var editActions = renderEditActions();

        return html("""
                <div class="toolbar secondary" data-signals="{cusipLookup: '${cusipValue}'}">
                    <div class="cusip-lookup">
                        <input type="text" data-bind="cusipLookup" placeholder="CUSIP"
                               data-on:keydown.enter="${navigateToCusip}">
                        <button data-on:click="${navigateToCusip}">Load</button>
                    </div>
                    <div class="toolbar-separator"></div>
                    <nav class="screen-nav">
                        ${styles}
                        ${generalLink}
                        ${incomeLink}
                        ${redemptionLink}
                    </nav>
                    <div class="edit-actions">${editActions}</div>
                </div>
                """,
                "cusipValue", cusipValue,
                "navigateToCusip", navigateToCusip,
                "styles", getStyles(),
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
                    @scope (#securities-screen) {
                        .cusip-lookup {
                            display: flex;
                            gap: var(--sp-xs);
                        }

                        .cusip-lookup input { width: 120px; }

                        .edit-actions { margin-left: auto; }

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

                        .schedule-empty {
                            color: var(--clr-text-muted);
                            font-style: italic;
                            padding: var(--sp-sm) 0;
                        }

                        .securities-empty {
                            padding: var(--sp-xl);
                            color: var(--clr-text-muted);
                            text-align: center;
                            font-size: var(--fs-lg);
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

    public synchronized ServerResponse saveEdit(ServerRequest request) throws Exception {
        var bond = request.body(Bond.class);
        bond.setCusip(editingBond.getCusip());
        bond.setLastCouponDate(editingBond.getLastCouponDate());
        bond.setResetSchedule(editingBond.getResetSchedule());
        bond.setCallSchedule(editingBond.getCallSchedule());
        bond.setPutSchedule(editingBond.getPutSchedule());
        bond.setSinkingFundSchedule(editingBond.getSinkingFundSchedule());
        bondRepository.saveBond(bond);
        currentBond = bond;
        editingBond = null;
        triggerUpdate();
        return ServerResponse.ok().build();
    }

    public synchronized ServerResponse cancelEdit() {
        editingBond = null;
        triggerUpdate();
        return ServerResponse.ok().build();
    }
}
