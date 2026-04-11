package com.bookie.screens;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondRepository;
import com.bookie.domain.entity.BondType;
import com.bookie.domain.entity.CouponType;
import com.bookie.domain.entity.DayCountConvention;
import com.bookie.infra.EscapedHtml;
import com.bookie.infra.SessionRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import com.bookie.components.DataGrid;
import static com.bookie.components.DataGrid.column;
import static com.bookie.components.DateInput.dateInput;
import static com.bookie.components.FormField.formField;
import static com.bookie.components.Link.link;
import static com.bookie.components.NumberInput.numberInput;
import static com.bookie.components.SelectInput.selectInput;
import static com.bookie.components.TextInput.textInput;
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
    private String currentSection;

    private boolean isEditing() { return editingBond != null; }

    private Bond getActiveBond() { return isEditing() ? editingBond : currentBond; }

    public SecuritiesScreen(BondRepository bondRepository) {
        super("Securities");
        this.bondRepository = bondRepository;
        this.currentSection = "general";
    }

    public static RouterFunction<ServerResponse> setupRoutes(SessionRegistry sessionRegistry) {
        return RouterFunctions.route()
                .GET("", _ -> ServerResponse.temporaryRedirect(
                        URI.create(RoutePrefix + "/" + NO_CUSIP + "/general")).build())
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

        currentSection = section.isBlank() ? "general" : section;

        if (NO_CUSIP.equals(cusip)) {
            currentCusip = NO_CUSIP;
            currentBond = null;
        } else {
            var bond = bondRepository.findBondByCusip(cusip);
            currentCusip = bond != null ? cusip : NO_CUSIP;
            currentBond = bond;
        }

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
            case "general" -> renderGeneral();
            case "income" -> renderIncome();
            case "redemption" -> renderRedemption();
            default -> EscapedHtml.blank();
        };
    }

    private EscapedHtml renderGeneral() {
        var bond = getActiveBond();
        var disabled = !isEditing();
        var bondTypeOptions = Arrays.stream(BondType.values()).map(BondType::name).toList();
        var bondTypeName = bond.getBondType() != null ? bond.getBondType().name() : null;

        return html("""
                <div class="form-fields bond-general">
                    <label>CUSIP<span class="cusip-display">${cusip}</span></label>
                    ${isin}
                    ${ticker}
                    ${issuerName}
                    ${description}
                    ${bondType}
                    ${sector}
                    ${currency}
                    ${country}
                    ${seniorityLevel}
                    ${issueDate}
                    ${datedDate}
                    ${maturityDate}
                    ${firstCouponDate}
                    ${issueSize}
                    ${faceValue}
                    ${issuePrice}
                    ${moodysRating}
                    ${spRating}
                    ${fitchRating}
                    ${secured}
                </div>
                """,
                "cusip", bond.getCusip(),
                "isin", formField("ISIN").withInput(textInput("isin", bond.getIsin()).withDisabled(disabled)),
                "ticker", formField("Ticker").withInput(textInput("ticker", bond.getTicker()).withDisabled(disabled)),
                "issuerName", formField("Issuer Name").withInput(textInput("issuerName", bond.getIssuerName()).withDisabled(disabled)),
                "description", formField("Description").withInput(textInput("description", bond.getDescription()).withDisabled(disabled)),
                "bondType", formField("Bond Type").withInput(selectInput("bondType", bondTypeOptions, bondTypeName).withDisabled(disabled)),
                "sector", formField("Sector").withInput(textInput("sector", bond.getSector()).withDisabled(disabled)),
                "currency", formField("Currency").withInput(textInput("currency", bond.getCurrency()).withDisabled(disabled)),
                "country", formField("Country").withInput(textInput("country", bond.getCountry()).withDisabled(disabled)),
                "seniorityLevel", formField("Seniority Level").withInput(textInput("seniorityLevel", bond.getSeniorityLevel()).withDisabled(disabled)),
                "issueDate", formField("Issue Date").withInput(dateInput("issueDate", bond.getIssueDate()).withDisabled(disabled)),
                "datedDate", formField("Dated Date").withInput(dateInput("datedDate", bond.getDatedDate()).withDisabled(disabled)),
                "maturityDate", formField("Maturity Date").withInput(dateInput("maturityDate", bond.getMaturityDate()).withDisabled(disabled)),
                "firstCouponDate", formField("First Coupon Date").withInput(dateInput("firstCouponDate", bond.getFirstCouponDate()).withDisabled(disabled)),
                "issueSize", formField("Issue Size").withInput(numberInput("issueSize", bond.getIssueSize()).withDisabled(disabled)),
                "faceValue", formField("Face Value").withInput(numberInput("faceValue", bond.getFaceValue()).withDisabled(disabled)),
                "issuePrice", formField("Issue Price").withInput(numberInput("issuePrice", bond.getIssuePrice()).withDisabled(disabled)),
                "moodysRating", formField("Moody's Rating").withInput(textInput("moodysRating", bond.getMoodysRating()).withDisabled(disabled)),
                "spRating", formField("S&P Rating").withInput(textInput("spRating", bond.getSpRating()).withDisabled(disabled)),
                "fitchRating", formField("Fitch Rating").withInput(textInput("fitchRating", bond.getFitchRating()).withDisabled(disabled)),
                "secured", formField("Secured").withInput(selectInput("secured", List.of("true", "false"), String.valueOf(bond.isSecured())).withDisabled(disabled)));
    }

    private EscapedHtml renderIncome() {
        var bond = getActiveBond();
        var disabled = !isEditing();
        var couponTypeOptions = Arrays.stream(CouponType.values()).map(CouponType::name).toList();
        var dayCountOptions = Arrays.stream(DayCountConvention.values()).map(DayCountConvention::name).toList();
        var couponTypeName = bond.getCouponType() != null ? bond.getCouponType().name() : null;
        var dayCountName = bond.getDayCount() != null ? bond.getDayCount().name() : null;
        var resetSchedule = bond.getResetSchedule();

        var resetTable = (resetSchedule == null || resetSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No reset schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Reset Date", Bond.ResetEntry::resetDate),
                        column("New Rate", Bond.ResetEntry::newRate))
                        .withRows(resetSchedule)
                        .render();

        return html("""
                <div class="bond-income">
                    <div class="form-fields">
                        ${couponType}
                        ${coupon}
                        ${spread}
                        ${couponFrequency}
                        ${dayCount}
                        ${floatingIndex}
                    </div>
                    ${resetTable}
                </div>
                """,
                "couponType", formField("Coupon Type").withInput(selectInput("couponType", couponTypeOptions, couponTypeName).withDisabled(disabled)),
                "coupon", formField("Coupon").withInput(numberInput("coupon", bond.getCoupon()).withDisabled(disabled)),
                "spread", formField("Spread").withInput(numberInput("spread", bond.getSpread()).withDisabled(disabled)),
                "couponFrequency", formField("Coupon Frequency").withInput(numberInput("couponFrequency", bond.getCouponFrequency()).withDisabled(disabled)),
                "dayCount", formField("Day Count").withInput(selectInput("dayCount", dayCountOptions, dayCountName).withDisabled(disabled)),
                "floatingIndex", formField("Floating Index").withInput(textInput("floatingIndex", bond.getFloatingIndex()).withDisabled(disabled)),
                "resetTable", resetTable);
    }

    private EscapedHtml renderRedemption() {
        var bond = getActiveBond();

        var callSchedule = bond.getCallSchedule();
        var callTable = (callSchedule == null || callSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No call schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Call Date", Bond.CallEntry::callDate),
                        column("Call Price", Bond.CallEntry::callPrice))
                        .withRows(callSchedule)
                        .render();

        var putSchedule = bond.getPutSchedule();
        var putTable = (putSchedule == null || putSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No put schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Put Date", Bond.PutEntry::putDate),
                        column("Put Price", Bond.PutEntry::putPrice))
                        .withRows(putSchedule)
                        .render();

        var sinkingFundSchedule = bond.getSinkingFundSchedule();
        var sinkingFundTable = (sinkingFundSchedule == null || sinkingFundSchedule.isEmpty())
                ? html("""
                        <p class="schedule-empty">No sinking fund schedule.</p>
                        """)
                : DataGrid.withColumns(
                        column("Sink Date", Bond.SinkingFundEntry::sinkDate),
                        column("Amount", Bond.SinkingFundEntry::amount))
                        .withRows(sinkingFundSchedule)
                        .render();

        return html("""
                <div class="bond-redemption">
                    <h3>Call Schedule</h3>
                    ${callTable}
                    <h3>Put Schedule</h3>
                    ${putTable}
                    <h3>Sinking Fund</h3>
                    ${sinkingFundTable}
                </div>
                """,
                "callTable", callTable,
                "putTable", putTable,
                "sinkingFundTable", sinkingFundTable);
    }

    private EscapedHtml renderSecondaryToolbar() {
        var tabId = getTabID().localID();
        var cusipValue = NO_CUSIP.equals(currentCusip) ? "" : currentCusip;
        var generalLink = link("securities/" + currentCusip + "/general", "General", tabId).withActive("general".equals(currentSection));
        var incomeLink = link("securities/" + currentCusip + "/income", "Income", tabId).withActive("income".equals(currentSection));
        var redemptionLink = link("securities/" + currentCusip + "/redemption", "Redemption", tabId).withActive("redemption".equals(currentSection));
        var navigateToCusip = "if($cusipLookup.trim()) window.location.href='/securities/'+$cusipLookup.trim()+'/" + currentSection + "?tabID=" + tabId + "'";
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
