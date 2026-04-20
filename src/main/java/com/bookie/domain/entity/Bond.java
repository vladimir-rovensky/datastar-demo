package com.bookie.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Bond implements Cloneable {

    private String cusip;
    private String isin;
    private String ticker;
    private String issuerName;
    private String description;

    private BondType bondType;
    private String sector;
    private String currency;
    private String country;

    private LocalDate issueDate;
    private LocalDate datedDate;
    private LocalDate maturityDate;
    private LocalDate firstCouponDate;
    private LocalDate lastCouponDate;

    private BigDecimal issueSize;
    private BigDecimal faceValue;
    private BigDecimal issuePrice;

    private CouponType couponType;
    private BigDecimal coupon;
    private int couponFrequency;
    private DayCountConvention dayCount;
    private String floatingIndex;
    private BigDecimal spread;
    private List<ResetEntry> resetSchedule = new ArrayList<>();
    private List<CallEntry> callSchedule = new ArrayList<>();
    private List<PutEntry> putSchedule = new ArrayList<>();
    private List<SinkingFundEntry> sinkingFundSchedule = new ArrayList<>();

    private int version = 0;

    private String moodysRating;
    private String spRating;
    private String fitchRating;
    private boolean secured;
    private String seniorityLevel;

    public Bond() {}

    public BigDecimal getOutstandingAmount() {
        if (getIssueSize() == null) {
            return null;
        }
        var today = LocalDate.now();
        BigDecimal sunkAmount = BigDecimal.ZERO;
        for (var entry : getSinkingFundSchedule()) {
            if (entry.getSinkDate() == null || entry.getAmount() == null) {
                return null;
            }
            if (!entry.getSinkDate().isAfter(today)) {
                sunkAmount = sunkAmount.add(entry.getAmount());
            }
        }
        return getIssueSize().subtract(sunkAmount);
    }

    @Override
    public Bond clone() {
        try {
            Bond copy = (Bond) super.clone();
            if (resetSchedule != null) {
                copy.resetSchedule = new ArrayList<>(resetSchedule.stream()
                        .map(e -> new ResetEntry(e.getId(), e.getResetDate(), e.getNewRate()))
                        .toList());
            }
            if (callSchedule != null) {
                copy.callSchedule = new ArrayList<>(callSchedule.stream()
                        .map(e -> new CallEntry(e.getId(), e.getCallDate(), e.getCallPrice()))
                        .toList());
            }
            if (putSchedule != null) {
                copy.putSchedule = new ArrayList<>(putSchedule.stream()
                        .map(e -> new PutEntry(e.getId(), e.getPutDate(), e.getPutPrice()))
                        .toList());
            }
            if (sinkingFundSchedule != null) {
                copy.sinkingFundSchedule = new ArrayList<>(sinkingFundSchedule.stream()
                        .map(e -> new SinkingFundEntry(e.getId(), e.getSinkDate(), e.getAmount()))
                        .toList());
            }
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public static class ResetEntry {
        private String id;
        private LocalDate resetDate;
        private BigDecimal newRate;

        public ResetEntry(String id, LocalDate resetDate, BigDecimal newRate) {
            this.id = id;
            this.resetDate = resetDate;
            this.newRate = newRate;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public LocalDate getResetDate() { return resetDate; }
        public void setResetDate(LocalDate resetDate) { this.resetDate = resetDate; }

        public BigDecimal getNewRate() { return newRate; }
        public void setNewRate(BigDecimal newRate) { this.newRate = newRate; }
    }

    public static class CallEntry {
        private String id;
        private LocalDate callDate;
        private BigDecimal callPrice;

        public CallEntry(String id, LocalDate callDate, BigDecimal callPrice) {
            this.id = id;
            this.callDate = callDate;
            this.callPrice = callPrice;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public LocalDate getCallDate() { return callDate; }
        public void setCallDate(LocalDate callDate) { this.callDate = callDate; }

        public BigDecimal getCallPrice() { return callPrice; }
        public void setCallPrice(BigDecimal callPrice) { this.callPrice = callPrice; }
    }

    public static class PutEntry {
        private String id;
        private LocalDate putDate;
        private BigDecimal putPrice;

        public PutEntry(String id, LocalDate putDate, BigDecimal putPrice) {
            this.id = id;
            this.putDate = putDate;
            this.putPrice = putPrice;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public LocalDate getPutDate() { return putDate; }
        public void setPutDate(LocalDate putDate) { this.putDate = putDate; }

        public BigDecimal getPutPrice() { return putPrice; }
        public void setPutPrice(BigDecimal putPrice) { this.putPrice = putPrice; }
    }

    public static class SinkingFundEntry {
        private String id;
        private LocalDate sinkDate;
        private BigDecimal amount;

        public SinkingFundEntry(String id, LocalDate sinkDate, BigDecimal amount) {
            this.id = id;
            this.sinkDate = sinkDate;
            this.amount = amount;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public LocalDate getSinkDate() { return sinkDate; }
        public void setSinkDate(LocalDate sinkDate) { this.sinkDate = sinkDate; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }

    public int getVersion() { return version; }
    public Bond setVersion(int version) { this.version = version; return this; }

    public String getCusip() { return cusip; }
    public Bond setCusip(String cusip) { this.cusip = cusip; return this; }

    public String getIsin() { return isin; }
    public Bond setIsin(String isin) { this.isin = isin; return this; }

    public String getTicker() { return ticker; }
    public Bond setTicker(String ticker) { this.ticker = ticker; return this; }

    public String getIssuerName() { return issuerName; }
    public Bond setIssuerName(String issuerName) { this.issuerName = issuerName; return this; }

    public String getDescription() { return description; }
    public Bond setDescription(String description) { this.description = description; return this; }

    public BondType getBondType() { return bondType; }
    public Bond setBondType(BondType bondType) { this.bondType = bondType; return this; }

    public String getSector() { return sector; }
    public Bond setSector(String sector) { this.sector = sector; return this; }

    public String getCurrency() { return currency; }
    public Bond setCurrency(String currency) { this.currency = currency; return this; }

    public String getCountry() { return country; }
    public Bond setCountry(String country) { this.country = country; return this; }

    public LocalDate getIssueDate() { return issueDate; }
    public Bond setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; return this; }

    public LocalDate getDatedDate() { return datedDate; }
    public Bond setDatedDate(LocalDate datedDate) { this.datedDate = datedDate; return this; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public Bond setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; return this; }

    public LocalDate getFirstCouponDate() { return firstCouponDate; }
    public Bond setFirstCouponDate(LocalDate firstCouponDate) { this.firstCouponDate = firstCouponDate; return this; }

    public LocalDate getLastCouponDate() { return lastCouponDate; }
    public Bond setLastCouponDate(LocalDate lastCouponDate) { this.lastCouponDate = lastCouponDate; return this; }

    public BigDecimal getIssueSize() { return issueSize; }
    public Bond setIssueSize(BigDecimal issueSize) { this.issueSize = issueSize; return this; }

    public BigDecimal getFaceValue() { return faceValue; }
    public Bond setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; return this; }

    public BigDecimal getIssuePrice() { return issuePrice; }
    public Bond setIssuePrice(BigDecimal issuePrice) { this.issuePrice = issuePrice; return this; }

    public CouponType getCouponType() { return couponType; }
    public Bond setCouponType(CouponType couponType) { this.couponType = couponType; return this; }

    public BigDecimal getCoupon() { return coupon; }
    public Bond setCoupon(BigDecimal coupon) { this.coupon = coupon; return this; }

    public int getCouponFrequency() { return couponFrequency; }
    public Bond setCouponFrequency(int couponFrequency) { this.couponFrequency = couponFrequency; return this; }

    public DayCountConvention getDayCount() { return dayCount; }
    public Bond setDayCount(DayCountConvention dayCount) { this.dayCount = dayCount; return this; }

    public String getFloatingIndex() { return floatingIndex; }
    public Bond setFloatingIndex(String floatingIndex) { this.floatingIndex = floatingIndex; return this; }

    public BigDecimal getSpread() { return spread; }
    public Bond setSpread(BigDecimal spread) { this.spread = spread; return this; }

    public List<ResetEntry> getResetSchedule() { return resetSchedule; }
    public Bond setResetSchedule(List<ResetEntry> resetSchedule) { this.resetSchedule = resetSchedule; return this; }

    public List<CallEntry> getCallSchedule() { return callSchedule; }
    public Bond setCallSchedule(List<CallEntry> callSchedule) { this.callSchedule = callSchedule; return this; }

    public List<PutEntry> getPutSchedule() { return putSchedule; }
    public Bond setPutSchedule(List<PutEntry> putSchedule) { this.putSchedule = putSchedule; return this; }

    public List<SinkingFundEntry> getSinkingFundSchedule() { return sinkingFundSchedule; }
    public Bond setSinkingFundSchedule(List<SinkingFundEntry> sinkingFundSchedule) { this.sinkingFundSchedule = sinkingFundSchedule; return this; }

    public String getMoodysRating() { return moodysRating; }
    public Bond setMoodysRating(String moodysRating) { this.moodysRating = moodysRating; return this; }

    public String getSpRating() { return spRating; }
    public Bond setSpRating(String spRating) { this.spRating = spRating; return this; }

    public String getFitchRating() { return fitchRating; }
    public Bond setFitchRating(String fitchRating) { this.fitchRating = fitchRating; return this; }

    public boolean isSecured() { return secured; }
    public Bond setSecured(boolean secured) { this.secured = secured; return this; }

    public String getSeniorityLevel() { return seniorityLevel; }
    public Bond setSeniorityLevel(String seniorityLevel) { this.seniorityLevel = seniorityLevel; return this; }
}