package com.bookie.domain.entity;

import com.bookie.infra.EventBus;
import com.bookie.infra.events.BondSavedEvent;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static com.bookie.infra.Validators.*;

@Repository
public class BondRepository {

    private final BondDAO dao;
    private final AtomicLong scheduleIdCounter;
    private final EventBus eventBus;

    private boolean generateFakeData = true;

    public static final String TEST_CUSIP = "000000AA0";

    public BondRepository(BondDAO dao, EventBus eventBus) {
        this.eventBus = eventBus;

        this.dao = dao;
        this.dao.saveAll(generateData());

        this.scheduleIdCounter = new AtomicLong(5000);
    }

    public Bond.ResetEntry createResetEntry() {
        return new Bond.ResetEntry(String.valueOf(scheduleIdCounter.getAndIncrement()), null, null);
    }

    public Bond.CallEntry createCallEntry() {
        return new Bond.CallEntry(String.valueOf(scheduleIdCounter.getAndIncrement()), null, null);
    }

    public Bond.PutEntry createPutEntry() {
        return new Bond.PutEntry(String.valueOf(scheduleIdCounter.getAndIncrement()), null, null);
    }

    public Bond.SinkingFundEntry createSinkingFundEntry() {
        return new Bond.SinkingFundEntry(String.valueOf(scheduleIdCounter.getAndIncrement()), null, null);
    }

    public synchronized void saveBond(Bond bond) {
        if(!isValid(bond)) {
            throw new RuntimeException("Tried to save an invalid bond.");
        }

        bond.setVersion(bond.getVersion() + 1);
        dao.save(bond);
        eventBus.publish(new BondSavedEvent(bond));
    }

    public List<Bond> getAllBonds() {
        return dao.findAll();
    }

    public Bond findBondByCusip(String cusip) {
        return dao.findByCusip(cusip);
    }

    public Map<String, Bond> findBondsByCusips(Collection<String> cusips) {
        return dao.findByCusips(cusips);
    }

    public boolean isValidCusip(String cusip) {
        //Pretend this is a bloom filter or something clever. Pretend to be impressed.
        return getAllBonds().stream().anyMatch(b -> Objects.equals(b.getCusip(), cusip));
    }

    public boolean isValid(Bond bond) {
        return validateIsin(bond.getIsin()) == null
                && validateTicker(bond.getTicker()) == null
                && validateIssuerName(bond.getIssuerName()) == null
                && validateBondType(bond.getBondType()) == null
                && validateCurrency(bond.getCurrency()) == null
                && validateCountry(bond.getCountry()) == null
                && validateIssueDate(bond.getIssueDate()) == null
                && validateMaturityDate(bond.getMaturityDate(), bond.getIssueDate()) == null
                && validateFirstCouponDate(bond.getFirstCouponDate(), bond.getIssueDate(), bond.getMaturityDate()) == null
                && validateFaceValue(bond.getFaceValue()) == null
                && validateIssuePrice(bond.getIssuePrice()) == null
                && validateIssueSize(bond.getIssueSize()) == null
                && validateCouponType(bond.getCouponType(), bond.getResetSchedule()) == null
                && validateCouponFrequency(bond.getCouponFrequency()) == null
                && validateDayCount(bond.getDayCount()) == null
                && validateCoupon(bond.getCoupon(), bond.getCouponType()) == null
                && validateFloatingIndex(bond.getFloatingIndex(), bond.getCouponType()) == null
                && bond.getResetSchedule().stream().allMatch(e ->
                        validateResetScheduleDate(e.getResetDate(), bond.getResetSchedule()) == null
                        && validateResetScheduleRate(e.getNewRate()) == null)
                && bond.getCallSchedule().stream().allMatch(e ->
                        validateCallScheduleDate(e.getCallDate(), bond.getCallSchedule()) == null
                        && validateCallSchedulePrice(e.getCallPrice()) == null)
                && bond.getPutSchedule().stream().allMatch(e ->
                        validatePutScheduleDate(e.getPutDate(), bond.getPutSchedule()) == null
                        && validatePutSchedulePrice(e.getPutPrice()) == null)
                && bond.getSinkingFundSchedule().stream().allMatch(e ->
                        validateSinkingFundScheduleDate(e.getSinkDate(), bond.getSinkingFundSchedule()) == null
                        && validateSinkingFundScheduleAmount(e.getAmount()) == null);
    }

    public String validateIsin(String isin) {
        return required(isin);
    }

    public String validateTicker(String ticker) {
        return required(ticker);
    }

    public String validateIssuerName(String issuerName) {
        return required(issuerName);
    }

    public String validateBondType(BondType bondType) {
        return required(bondType);
    }

    public String validateCurrency(String currency) {
        return required(currency);
    }

    public String validateCountry(String country) {
        return required(country);
    }

    public String validateIssueDate(LocalDate issueDate) {
        return required(issueDate);
    }

    public String validateMaturityDate(LocalDate maturityDate, LocalDate issueDate) {
        return and(required(maturityDate), after(maturityDate, issueDate, "issue date"));
    }

    public String validateFirstCouponDate(LocalDate firstCouponDate, LocalDate issueDate, LocalDate maturityDate) {
        if (firstCouponDate == null) {
            return null;
        }
        return and(after(firstCouponDate, issueDate, "issue date"), before(firstCouponDate, maturityDate, "maturity date"));
    }

    public String validateFaceValue(BigDecimal faceValue) {
        return and(required(faceValue), greaterThan(faceValue, BigDecimal.ZERO));
    }

    public String validateIssuePrice(BigDecimal issuePrice) {
        return and(required(issuePrice), greaterThan(issuePrice, BigDecimal.ZERO));
    }

    public String validateIssueSize(BigDecimal issueSize) {
        return and(required(issueSize), atLeast(issueSize, BigDecimal.ZERO));
    }

    public String validateCouponType(CouponType couponType, List<Bond.ResetEntry> resetSchedule) {
        var error = required(couponType);
        if (error != null) {
            return error;
        }
        if (couponType == CouponType.FLOATING && (resetSchedule == null || resetSchedule.isEmpty())) {
            return "At least one reset entry is required for floating rate bonds";
        }
        return null;
    }

    private static final Set<Integer> VALID_COUPON_FREQUENCIES = Set.of(1, 2, 4, 12);

    public String validateCouponFrequency(int couponFrequency) {
        return oneOf(couponFrequency, VALID_COUPON_FREQUENCIES);
    }

    public String validateDayCount(DayCountConvention dayCount) {
        return required(dayCount);
    }

    public String validateCoupon(BigDecimal coupon, CouponType couponType) {
        if (couponType != CouponType.FIXED) {
            return null;
        }
        if (coupon == null) {
            return "Required for fixed rate bonds";
        }
        return null;
    }

    public String validateFloatingIndex(String floatingIndex, CouponType couponType) {
        if (couponType != CouponType.FLOATING) {
            return null;
        }
        if (floatingIndex == null || floatingIndex.isBlank()) {
            return "Required for floating rate bonds";
        }
        return null;
    }

    public String validateResetScheduleDate(LocalDate resetDate, List<Bond.ResetEntry> allEntries) {
        return and(required(resetDate), unique(resetDate, allEntries.stream().map(Bond.ResetEntry::getResetDate)));
    }

    public String validateResetScheduleRate(BigDecimal newRate) {
        return and(required(newRate), atLeast(newRate, BigDecimal.ZERO));
    }

    public String validateCallScheduleDate(LocalDate callDate, List<Bond.CallEntry> allEntries) {
        return and(required(callDate), unique(callDate, allEntries.stream().map(Bond.CallEntry::getCallDate)));
    }

    public String validateCallSchedulePrice(BigDecimal callPrice) {
        return and(required(callPrice), atLeast(callPrice, BigDecimal.ZERO));
    }

    public String validatePutScheduleDate(LocalDate putDate, List<Bond.PutEntry> allEntries) {
        return and(required(putDate), unique(putDate, allEntries.stream().map(Bond.PutEntry::getPutDate)));
    }

    public String validatePutSchedulePrice(BigDecimal putPrice) {
        return and(required(putPrice), atLeast(putPrice, BigDecimal.ZERO));
    }

    public String validateSinkingFundScheduleDate(LocalDate sinkDate, List<Bond.SinkingFundEntry> allEntries) {
        return and(required(sinkDate), unique(sinkDate, allEntries.stream().map(Bond.SinkingFundEntry::getSinkDate)));
    }

    public String validateSinkingFundScheduleAmount(BigDecimal amount) {
        return and(required(amount), atLeast(amount, BigDecimal.ZERO));
    }

    private List<Bond> generateData() {
        List<Bond> list = new ArrayList<>();

        if(!generateFakeData) {
            return list;
        }

        Object[][] corporates = {
                {"037833100", "US0378331005", "AAPL",  "Apple Inc.",                   "Technology",        "Baa1", "AA+",  "AA+",  5.50, 2028},
                {"38141GXZ2", "US38141GXZ20", "JPM",  "JPMorgan Chase & Co.",         "Financials",        "A2",   "A-",   "A+",   5.00, 2030},
                {"166764BG5", "US166764BG59", "CVX",  "Chevron Corporation",           "Energy",            "Aa2",  "AA",   "AA",   4.95, 2033},
                {"345370CP7", "US345370CP72", "F",    "Ford Motor Company",            "Consumer Cyclical", "Ba2",  "BB+",  "BB+",  6.10, 2029},
                {"00206RCW1", "US00206RCW14", "T",    "AT&T Inc.",                     "Communication",     "Baa2", "BBB",  "BBB",  4.35, 2032},
                {"59156RBQ9", "US59156RBQ95", "MET",  "MetLife Inc.",                  "Financials",        "A3",   "A-",   "A-",   5.25, 2034},
                {"723254CL1", "US723254CL19", "PFE",  "Pfizer Inc.",                   "Healthcare",        "A2",   "A",    "A",    4.75, 2031},
                {"911312BK0", "US911312BK08", "UPS",  "United Parcel Service Inc.",    "Industrials",       "A3",   "A-",   "A-",   3.90, 2029},
                {"126650DE8", "US126650DE82", "CVS",  "CVS Health Corporation",        "Healthcare",        "Baa2", "BBB",  "BBB",  5.05, 2033},
                {"20030NCK5", "US20030NCK59", "CMCSA","Comcast Corporation",           "Communication",     "A3",   "A-",   "A-",   4.60, 2030},
                {"14040HBL8", "US14040HBL83", "CAT",  "Caterpillar Inc.",              "Industrials",       "A2",   "A",    "A",    3.80, 2028},
                {"235851AX5", "US235851AX53", "DAL",  "Delta Air Lines Inc.",          "Industrials",       "Ba1",  "BB+",  "BB",   7.00, 2029},
                {"36962G4H3", "US36962G4H35", "GE",   "GE Capital International",     "Financials",        "Baa1", "BBB+", "BBB+", 5.55, 2035},
                {"48128BAD6", "US48128BAD63", "JNJ",  "Johnson & Johnson",             "Healthcare",        "Aaa",  "AAA",  "AAA",  3.55, 2030},
                {"743315AZ4", "US743315AZ48", "PG",   "Procter & Gamble Co.",          "Consumer Staples",  "Aa3",  "AA-",  "AA-",  4.10, 2032},
                {"857477AS3", "US857477AS38", "STT",  "State Street Corporation",      "Financials",        "A1",   "A",    "A",    4.80, 2031},
                {"654106AK7", "US654106AK79", "NKE",  "Nike Inc.",                     "Consumer Cyclical", "A1",   "AA-",  "AA-",  3.25, 2027},
                {"92826CAB7", "US92826CAB78", "V",    "Visa Inc.",                     "Financials",        "Aa3",  "AA-",  "AA-",  4.30, 2033},
                {"594918BP8", "US594918BP84", "MSFT", "Microsoft Corporation",         "Technology",        "Aaa",  "AAA",  "AAA",  3.45, 2031},
                {"02079KAB7", "US02079KAB72", "GOOG", "Alphabet Inc.",                 "Technology",        "Aa2",  "AA+",  "AA",   3.70, 2030},
                {"084670BK4", "US084670BK40", "BRK",  "Berkshire Hathaway Inc.",       "Financials",        "Aa2",  "AA",   "AA",   4.20, 2048},
                {"717081DM2", "US717081DM24", "PEP",  "PepsiCo Inc.",                  "Consumer Staples",  "A1",   "A+",   "A+",   4.00, 2029},
                {"931142EL3", "US931142EL34", "WMT",  "Walmart Inc.",                  "Consumer Staples",  "Aa2",  "AA",   "AA",   4.15, 2034},
                {"24703TAA8", "US24703TAA81", "DE",   "Deere & Company",               "Industrials",       "A2",   "A",    "A",    4.90, 2032},
                {"375558BX3", "US375558BX37", "GS",   "Goldman Sachs Group Inc.",      "Financials",        "A2",   "BBB+", "A",    5.80, 2033},
                {"46625HJK1", "US46625HJK10", "JPM2", "JPMorgan Chase & Co.",         "Financials",        "A2",   "A-",   "A+",   5.30, 2035},
                {"136385AS9", "US136385AS96", "CAP",  "Capitalink Corp.",              "Financials",        "Ba3",  "BB-",  "BB-",  8.25, 2029},
                {"760677AX3", "US760677AX38", "RF",   "Regions Financial Corp.",       "Financials",        "Baa1", "BBB+", "BBB+", 5.75, 2030},
                {"674599CF2", "US674599CF25", "OXY",  "Occidental Petroleum Corp.",    "Energy",            "Ba1",  "BB+",  "BB+",  6.45, 2031},
                {"65339FAD5", "US65339FAD54", "NEM",  "Newmont Corporation",           "Materials",         "Baa2", "BBB",  "BBB",  5.15, 2033},
                {"89788JAC3", "US89788JAC32", "TSLA", "Tesla Inc.",                    "Consumer Cyclical", "Ba1",  "BB+",  "BB+",  5.30, 2028},
                {"071734AX0", "US071734AX08", "BA",   "Boeing Company",                "Industrials",       "Baa2", "BBB-", "BBB-", 5.93, 2060},
                {"94974BGL0", "US94974BGL08", "WFC",  "Wells Fargo & Company",         "Financials",        "A1",   "A+",   "A+",   4.90, 2032},
                {"02635PJT8", "US02635PJT86", "AXP",  "American Express Company",      "Financials",        "A2",   "BBB+", "A-",   5.10, 2031},
                {"13063XAD4", "US13063XAD40", "CA",   "CA Inc.",                       "Technology",        "Baa2", "BBB",  "BBB",  4.70, 2030},
                {"88732JAB3", "US88732JAB38", "TGT",  "Target Corporation",            "Consumer Staples",  "A2",   "A",    "A",    3.90, 2029},
                {"693475AJ5", "US693475AJ57", "PPL",  "PPL Capital Funding Inc.",      "Utilities",         "Baa1", "BBB+", "BBB+", 4.00, 2027},
                {"45867GAD3", "US45867GAD37", "IBM",  "IBM Corporation",               "Technology",        "A3",   "A-",   "A-",   3.60, 2028},
                {"31428XBH8", "US31428XBH82", "FDUS", "Fidelity National Info Svcs",  "Technology",        "Baa2", "BBB",  "BBB",  5.63, 2033},
                {"532457BM0", "US532457BM09", "LLY",  "Eli Lilly and Company",         "Healthcare",        "A2",   "A+",   "A+",   4.50, 2032},
                {"56585AAQ0", "US56585AAQ08", "MA",   "Mastercard Incorporated",       "Financials",        "A1",   "A+",   "A+",   3.85, 2029},
                {"72352LAG2", "US72352LAG26", "PM",   "Philip Morris International",   "Consumer Staples",  "A2",   "A",    "A-",   5.13, 2031},
                {"464288FR6", "US464288FR68", "IP",   "International Paper Company",   "Materials",         "Baa2", "BBB-", "BBB",  6.00, 2041},
                {"531229AT6", "US531229AT64", "LMT",  "Lockheed Martin Corporation",   "Industrials",       "Baa1", "A-",   "A-",   4.15, 2030},
                {"89832QAD3", "US89832QAD31", "TROW", "T. Rowe Price Group Inc.",      "Financials",        "A1",   "A+",   "A",    4.00, 2029},
                {"437076BM4", "US437076BM42", "HD",   "Home Depot Inc.",               "Consumer Cyclical", "A2",   "A",    "A",    4.20, 2032},
                {"92343VAA9", "US92343VAA99", "VZ",   "Verizon Communications Inc.",   "Communication",     "Baa1", "BBB+", "BBB+", 4.75, 2034},
                {"023608AC0", "US023608AC07", "AMZ",  "Amazon.com Inc.",               "Consumer Cyclical", "A1",   "AA",   "AA-",  3.15, 2027},
                {"31677QBG8", "US31677QBG85", "FDX",  "FedEx Corporation",             "Industrials",       "Baa2", "BBB",  "BBB",  4.55, 2030},
                {"084670CJ4", "US084670CJ45", "BRK2", "Berkshire Hathaway Finance",    "Financials",        "Aa2",  "AA",   "AA",   4.75, 2045},
        };

        Object[][] munis = {
                {"13063B5G0", "US13063B5G05", "CAGO",  "State of California GO",              "Government",    "Aa2",  "AA-",  "AA",   3.50, 2040},
                {"7954521G3", "US7954521G37", "NYTX",  "New York City Transitional Finance",  "Government",    "Aa1",  "AA",   "AA+",  3.25, 2037},
                {"13034PCM6", "US13034PCM67", "CALW",  "California Water Service",            "Utilities",     "Aa3",  "AA-",  "AA-",  3.75, 2042},
                {"64985XAD1", "US64985XAD18", "NYCHA", "New York City Housing Auth.",         "Government",    "Aa2",  "AA",   "AA",   3.40, 2038},
                {"452252BT7", "US452252BT73", "ILGO",  "State of Illinois GO",                "Government",    "Baa3", "BBB-", "BBB",  5.10, 2035},
                {"677520AA4", "US677520AA47", "TXMUN", "City of Houston TX",                  "Government",    "Aa3",  "AA",   "AA-",  3.20, 2043},
                {"880591AJ5", "US880591AJ57", "TNPWR", "Tennessee Valley Authority",          "Utilities",     "Aaa",  "AAA",  "AAA",  3.00, 2035},
                {"452252CD1", "US452252CD19", "CHIGO", "City of Chicago GO",                  "Government",    "Ba1",  "BB+",  "BB+",  5.50, 2031},
                {"345370AA2", "US345370AA20", "FLGO",  "State of Florida GO",                 "Government",    "Aa1",  "AAA",  "AA+",  2.95, 2039},
                {"59156RBQ0", "US59156RBQ06", "MAGO",  "Commonwealth of Massachusetts GO",    "Government",    "Aa1",  "AA",   "AA+",  3.10, 2041},
                {"136385AA8", "US136385AA86", "COMED", "Commonwealth Edison Co.",             "Utilities",     "Baa1", "BBB+", "BBB+", 4.35, 2044},
                {"884903AC1", "US884903AC19", "THSPT", "Dallas-Fort Worth Airport Rev.",      "Government",    "A1",   "A+",   "A+",   3.80, 2048},
                {"46048BDB0", "US46048BDB07", "ISDED", "Iowa Student Loan Liquidity Corp.",   "Government",    "Aaa",  "AAA",  "AAA",  2.80, 2034},
                {"238813AH7", "US238813AH77", "DCWA",  "DC Water and Sewer Authority",        "Utilities",     "Aa2",  "AA",   "AA",   3.65, 2046},
                {"74267LAA5", "US74267LAA54", "PRAGO", "Puerto Rico GO",                      "Government",    "Ca",   "D",    "D",    5.00, 2035},
                {"084670AZ6", "US084670AZ69", "LAUSD", "Los Angeles Unified School Dist.",    "Government",    "Aa2",  "AA-",  "AA",   3.30, 2040},
                {"811156AA3", "US811156AA36", "SEAWA", "Seattle WA Water Rev.",               "Utilities",     "Aaa",  "AAA",  "AAA",  3.05, 2053},
                {"345519AC2", "US345519AC28", "DEPWR", "Delaware Electric Coop Rev.",         "Utilities",     "A2",   "A",    "A",    3.90, 2042},
                {"74529JAC4", "US74529JAC41", "PASCO", "Pascagoula MS Port Rev.",             "Government",    "A3",   "A-",   "A-",   4.10, 2038},
                {"64966EAB7", "US64966EAB74", "NYNMH", "NYC Health and Hospitals Corp.",      "Government",    "Aa2",  "AA",   "AA-",  3.55, 2036},
                {"69247BAC0", "US69247BAC08", "PGHWA", "Port of Greater Houston Auth.",       "Government",    "A1",   "AA-",  "A+",   3.70, 2050},
                {"87612EAA1", "US87612EAA18", "TAMD",  "Tampa Bay Water",                     "Utilities",     "Aaa",  "AAA",  "AAA",  2.90, 2044},
                {"89356BAC9", "US89356BAC97", "TRNS",  "Triborough Bridge & Tunnel Auth.",    "Government",    "Aa2",  "AA+",  "AA",   3.45, 2047},
                {"29280EAB5", "US29280EAB54", "ENPWR", "Entergy Louisiana LLC",               "Utilities",     "Baa2", "BBB",  "BBB",  4.20, 2033},
                {"584977AC7", "US584977AC79", "MDBDS", "Maryland Stadium Authority Rev.",     "Government",    "Aa2",  "AA",   "AA",   3.25, 2042},
                {"90932PAA3", "US90932PAA38", "UTAGO", "State of Utah GO",                    "Government",    "Aaa",  "AAA",  "AAA",  2.85, 2041},
                {"13063CAA0", "US13063CAA08", "CASF",  "California Statewide CDA",            "Government",    "A1",   "A+",   "A",    3.60, 2044},
                {"34153TAA7", "US34153TAA71", "FLPWR", "Florida Power & Light Rev.",          "Utilities",     "Aa2",  "A+",   "AA",   3.40, 2049},
                {"59259RAA5", "US59259RAA54", "MICHI", "Michigan Finance Authority",          "Government",    "Aa1",  "AA",   "AA+",  3.15, 2043},
                {"487612AA0", "US487612AA06", "KYGOB", "Commonwealth of Kentucky GO",         "Government",    "Aa3",  "A+",   "AA-",  3.55, 2037},
                {"64984AAA2", "US64984AAA28", "NJGO",  "State of New Jersey GO",              "Government",    "A3",   "BBB+", "A-",   4.00, 2034},
                {"738312AA1", "US738312AA18", "PORTOR","Port of Portland OR",                 "Government",    "A1",   "A+",   "A+",   3.70, 2048},
                {"198288AA4", "US198288AA46", "COAGO", "State of Colorado GO",                "Government",    "Aa1",  "AA+",  "AA",   2.90, 2045},
                {"650114AA8", "US650114AA86", "NMMUN", "New Mexico Finance Authority",        "Government",    "Aa2",  "AA",   "AA",   3.20, 2040},
                {"816851AA2", "US816851AA29", "SFPUC", "San Francisco PUC Rev.",              "Utilities",     "Aa2",  "AA",   "AA",   3.35, 2051},
                {"882635AA5", "US882635AA54", "TEXDOT","Texas Dept of Transportation",        "Government",    "Aaa",  "AAA",  "AAA",  2.95, 2046},
                {"34531TAA3", "US34531TAA37", "FLHWY", "Florida Turnpike Enterprise Rev.",    "Government",    "Aa3",  "AA-",  "AA-",  3.45, 2049},
                {"130645AA9", "US130645AA98", "CAHTH", "California Health Facilities",        "Government",    "A2",   "A",    "A",    4.05, 2038},
                {"89045TAA6", "US89045TAA62", "TNAGO", "State of Tennessee GO",               "Government",    "Aaa",  "AAA",  "AAA",  2.75, 2042},
                {"592479AA1", "US592479AA18", "MIDET", "Detroit MI Sewage Disposal",          "Utilities",     "A3",   "A-",   "A-",   4.30, 2036},
                {"167486AA3", "US167486AA38", "CHICA", "Chicago O'Hare Airport Rev.",         "Government",    "A1",   "A",    "A+",   4.00, 2043},
                {"883556AA7", "US883556AA75", "TNEDU", "Tennessee School Bond Auth.",         "Government",    "Aa1",  "AA+",  "AA+",  3.00, 2040},
                {"44329TAA5", "US44329TAA54", "HOUS",  "Houston TX Combined Utility",         "Utilities",     "Aa2",  "AA",   "AA",   3.50, 2045},
                {"135088AA3", "US135088AA30", "CABRT", "California Bay Area Rapid Transit",   "Government",    "Aa2",  "AA",   "AA",   3.10, 2048},
                {"754912AA8", "US754912AA87", "RALEGH","City of Raleigh NC GO",               "Government",    "Aaa",  "AAA",  "AAA",  2.80, 2043},
                {"287766AA0", "US287766AA09", "ELAGO", "State of Alabama GO",                 "Government",    "Aa1",  "AA",   "AA+",  3.05, 2041},
                {"677513AA3", "US677513AA37", "TXLOC", "Texas Local Government Pool",         "Government",    "Aaa",  "AAA",  "AAA",  2.70, 2039},
                {"74163TAA0", "US74163TAA07", "PRJWB", "Puerto Rico Aqueduct & Sewer",        "Utilities",     "Caa3", "CCC",  "CCC",  5.25, 2033},
                {"75281TAA2", "US75281TAA29", "RIOPR", "Rio Grande City TX ISD",              "Government",    "Aa2",  "AA",   "AA-",  3.65, 2038},
                {"384160AA2", "US384160AA28", "GAGO",  "State of Georgia GO",                 "Government",    "Aaa",  "AAA",  "AAA",  2.65, 2044},
                {"419792AA1", "US419792AA18", "HIREV", "Hawaii State GO",                     "Government",    "Aa2",  "AA+",  "AA",   3.00, 2040},
        };

        LocalDate today = LocalDate.of(2026, 4, 3);

        list.add(buildTestBond(today));
        for (Object[] row : corporates) {
            list.add(buildCorporate(row, today));
        }
        for (Object[] row : munis) {
            list.add(buildMuni(row, today));
        }

        return list;
    }

    private static Bond buildTestBond(LocalDate ignored) {
        LocalDate issueDate    = LocalDate.of(2022, 1, 15);
        LocalDate maturityDate = LocalDate.of(2032, 1, 15);

        Bond b = new Bond();
        b.setCusip(TEST_CUSIP);
        b.setIsin("US000000AA09");
        b.setTicker("TEST");
        b.setIssuerName("Test Issuer Corp.");
        b.setDescription("TEST SOFR+1.00 2032");
        b.setBondType(BondType.CORPORATE);
        b.setSector("Financials");
        b.setCurrency("USD");
        b.setCountry("US");
        b.setIssueDate(issueDate);
        b.setDatedDate(issueDate);
        b.setMaturityDate(maturityDate);
        b.setFirstCouponDate(issueDate.plusMonths(6));
        b.setLastCouponDate(maturityDate.minusMonths(6));
        b.setIssueSize(new BigDecimal("1000000000"));
        b.setFaceValue(new BigDecimal("1000"));
        b.setIssuePrice(new BigDecimal("99.500"));
        b.setCouponType(CouponType.FLOATING);
        b.setFloatingIndex("SOFR");
        b.setSpread(new BigDecimal("1.00"));
        b.setCoupon(new BigDecimal("6.35"));
        b.setCouponFrequency(4);
        b.setDayCount(DayCountConvention.ACT_360);
        b.setMoodysRating("A2");
        b.setSpRating("A");
        b.setFitchRating("A");
        b.setSecured(false);
        b.setSeniorityLevel("SENIOR_UNSECURED");

        b.setResetSchedule(buildResetSchedule(TEST_CUSIP, issueDate, maturityDate, new BigDecimal("1.00")));

        b.setCallSchedule(List.of(
                new Bond.CallEntry("0", LocalDate.of(2027, 1, 15), new BigDecimal("102.00")),
                new Bond.CallEntry("1", LocalDate.of(2028, 1, 15), new BigDecimal("101.50")),
                new Bond.CallEntry("2", LocalDate.of(2029, 1, 15), new BigDecimal("101.00")),
                new Bond.CallEntry("3", LocalDate.of(2030, 1, 15), new BigDecimal("100.50")),
                new Bond.CallEntry("4", LocalDate.of(2031, 1, 15), new BigDecimal("100.00"))
        ));

        b.setPutSchedule(List.of(
                new Bond.PutEntry("0", LocalDate.of(2025, 1, 15), new BigDecimal("100.00")),
                new Bond.PutEntry("1", LocalDate.of(2027, 1, 15), new BigDecimal("100.00")),
                new Bond.PutEntry("2", LocalDate.of(2029, 1, 15), new BigDecimal("100.00"))
        ));

        b.setSinkingFundSchedule(List.of(
                new Bond.SinkingFundEntry("0", LocalDate.of(2028, 1, 15), new BigDecimal("200000000")),
                new Bond.SinkingFundEntry("1", LocalDate.of(2029, 1, 15), new BigDecimal("200000000")),
                new Bond.SinkingFundEntry("2", LocalDate.of(2030, 1, 15), new BigDecimal("200000000")),
                new Bond.SinkingFundEntry("3", LocalDate.of(2031, 1, 15), new BigDecimal("200000000"))
        ));

        return b;
    }

    private static Bond buildCorporate(Object[] row, LocalDate today) {
        String cusip      = (String) row[0];
        String isin       = (String) row[1];
        String ticker     = (String) row[2];
        String issuer     = (String) row[3];
        String sector     = (String) row[4];
        String moodys     = (String) row[5];
        String sp         = (String) row[6];
        String fitch      = (String) row[7];
        double couponVal  = (double) row[8];
        int    matYear    = (int)    row[9];

        LocalDate issueDate    = today.minusYears(matYear - today.getYear()).minusYears(2);
        LocalDate maturityDate = LocalDate.of(matYear, issueDate.getMonth(), 15);
        LocalDate datedDate    = issueDate;
        LocalDate firstCoupon  = issueDate.plusMonths(6);

        Bond b = new Bond();
        b.setCusip(cusip);
        b.setIsin(isin);
        b.setTicker(ticker);
        b.setIssuerName(issuer);
        b.setDescription(ticker + " " + couponVal + " " + matYear);
        b.setBondType(BondType.CORPORATE);
        b.setSector(sector);
        b.setCurrency("USD");
        b.setCountry("US");
        b.setIssueDate(issueDate);
        b.setDatedDate(datedDate);
        b.setMaturityDate(maturityDate);
        b.setFirstCouponDate(firstCoupon);
        b.setLastCouponDate(maturityDate.minusMonths(6));
        b.setIssueSize(randomIssueSize(cusip, 500_000_000, 5_000_000_000L));
        b.setFaceValue(new BigDecimal("1000"));
        b.setIssuePrice(new BigDecimal("99.875"));
        if (isFloatingCorporate(cusip)) {
            BigDecimal floatSpread = deriveSpread(cusip);
            List<Bond.ResetEntry> resets = buildResetSchedule(cusip, issueDate, maturityDate, floatSpread);
            BigDecimal currentCoupon = resets.stream()
                    .filter(resetEntry -> !resetEntry.getResetDate().isAfter(today))
                    .reduce((ignored, second) -> second)
                    .map(Bond.ResetEntry::getNewRate)
                    .orElse(floatSpread);
            b.setCouponType(CouponType.FLOATING);
            b.setFloatingIndex("SOFR");
            b.setSpread(floatSpread);
            b.setCoupon(currentCoupon);
            b.setCouponFrequency(4);
            b.setDayCount(DayCountConvention.ACT_360);
            b.setResetSchedule(resets);
            b.setDescription(ticker + " SOFR+" + floatSpread + " " + matYear);
        } else {
            b.setCouponType(CouponType.FIXED);
            b.setCoupon(BigDecimal.valueOf(couponVal));
            b.setCouponFrequency(2);
            b.setDayCount(DayCountConvention.THIRTY_360);
        }
        b.setMoodysRating(moodys);
        b.setSpRating(sp);
        b.setFitchRating(fitch);
        b.setSecured(false);
        b.setSeniorityLevel("SENIOR_UNSECURED");

        if (matYear > 2031) {
            b.setCallSchedule(List.of(
                    new Bond.CallEntry("0", maturityDate.minusYears(5), new BigDecimal("101.00")),
                    new Bond.CallEntry("1", maturityDate.minusYears(2), new BigDecimal("100.50"))
            ));
        } else if (matYear >= 2028) {
            b.setCallSchedule(List.of(
                    new Bond.CallEntry("0", maturityDate.minusYears(1), new BigDecimal("100.00"))
            ));
        }

        if (cusip.hashCode() % 2 == 0) {
            b.setPutSchedule(List.of(
                    new Bond.PutEntry("0", issueDate.plusYears(3), new BigDecimal("100.00"))
            ));
        }

        if ("Industrials".equals(sector)) {
            b.setSinkingFundSchedule(List.of(
                    new Bond.SinkingFundEntry("0", maturityDate.minusYears(3), b.getIssueSize().multiply(new BigDecimal("0.25"))),
                    new Bond.SinkingFundEntry("1", maturityDate.minusYears(1), b.getIssueSize().multiply(new BigDecimal("0.25")))
            ));
        }

        return b;
    }

    private static boolean isFloatingCorporate(String cusip) {
        return Math.abs(cusip.hashCode()) % 3 == 0;
    }

    private static BigDecimal deriveSpread(String cusip) {
        long hash = Math.abs(cusip.hashCode());
        double spreadBps = 50 + (hash % 101);
        return BigDecimal.valueOf(Math.round(spreadBps) / 100.0).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static List<Bond.ResetEntry> buildResetSchedule(String cusip, LocalDate issueDate, LocalDate maturityDate, BigDecimal spread) {
        List<Bond.ResetEntry> resets = new ArrayList<>();
        long hash = Math.abs(cusip.hashCode());
        double baseSofr = 4.00 + (hash % 150) / 100.0;
        LocalDate resetDate = issueDate;
        int step = 0;
        while (!resetDate.isAfter(maturityDate)) {
            double oscillation = Math.sin(step * 0.4) * 0.30;
            double sofr = Math.max(0.01, baseSofr + oscillation);
            double allIn = sofr + spread.doubleValue();
            resets.add(new Bond.ResetEntry(String.valueOf(step), resetDate, BigDecimal.valueOf(Math.round(allIn * 100), 2)));
            resetDate = resetDate.plusMonths(3);
            step++;
        }
        return resets;
    }

    private static Bond buildMuni(Object[] row, LocalDate today) {
        String cusip      = (String) row[0];
        String isin       = (String) row[1];
        String ticker     = (String) row[2];
        String issuer     = (String) row[3];
        String sector     = (String) row[4];
        String moodys     = (String) row[5];
        String sp         = (String) row[6];
        String fitch      = (String) row[7];
        double couponVal  = (double) row[8];
        int    matYear    = (int)    row[9];

        LocalDate issueDate    = today.minusYears(matYear - today.getYear()).minusYears(3);
        LocalDate maturityDate = LocalDate.of(matYear, 7, 1);
        LocalDate datedDate    = issueDate;
        LocalDate firstCoupon  = issueDate.plusMonths(6);

        Bond b = new Bond();
        b.setCusip(cusip);
        b.setIsin(isin);
        b.setTicker(ticker);
        b.setIssuerName(issuer);
        b.setDescription(ticker + " " + couponVal + " " + matYear);
        b.setBondType(BondType.MUNI);
        b.setSector(sector);
        b.setCurrency("USD");
        b.setCountry("US");
        b.setIssueDate(issueDate);
        b.setDatedDate(datedDate);
        b.setMaturityDate(maturityDate);
        b.setFirstCouponDate(firstCoupon);
        b.setLastCouponDate(maturityDate.minusMonths(6));
        b.setIssueSize(randomIssueSize(cusip, 50_000_000, 500_000_000));
        b.setFaceValue(new BigDecimal("5000"));
        b.setIssuePrice(new BigDecimal("100.000"));
        b.setCouponType(CouponType.FIXED);
        b.setCoupon(BigDecimal.valueOf(couponVal));
        b.setCouponFrequency(2);
        b.setDayCount(DayCountConvention.ACT_ACT);
        b.setMoodysRating(moodys);
        b.setSpRating(sp);
        b.setFitchRating(fitch);
        b.setSecured(true);
        b.setSeniorityLevel("SENIOR_SECURED");
        return b;
    }

    private static BigDecimal randomIssueSize(String seed, long min, long max) {
        long hash = Math.abs(seed.hashCode());
        long range = max - min;
        long raw = min + (hash % range);
        long rounded = (raw / 50_000_000) * 50_000_000;
        return new BigDecimal(Math.max(rounded, min));
    }

    public void setGenerateFakeData(boolean generateFakeData) {
        this.generateFakeData = generateFakeData;
    }
}
