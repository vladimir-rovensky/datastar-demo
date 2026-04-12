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

    private String moodysRating;
    private String spRating;
    private String fitchRating;
    private boolean secured;
    private String seniorityLevel;

    public Bond() {}

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

    public String getCusip() { return cusip; }
    public void setCusip(String cusip) { this.cusip = cusip; }

    public String getIsin() { return isin; }
    public void setIsin(String isin) { this.isin = isin; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public String getIssuerName() { return issuerName; }
    public void setIssuerName(String issuerName) { this.issuerName = issuerName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BondType getBondType() { return bondType; }
    public void setBondType(BondType bondType) { this.bondType = bondType; }

    public String getSector() { return sector; }
    public void setSector(String sector) { this.sector = sector; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDatedDate() { return datedDate; }
    public void setDatedDate(LocalDate datedDate) { this.datedDate = datedDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public LocalDate getFirstCouponDate() { return firstCouponDate; }
    public void setFirstCouponDate(LocalDate firstCouponDate) { this.firstCouponDate = firstCouponDate; }

    public LocalDate getLastCouponDate() { return lastCouponDate; }
    public void setLastCouponDate(LocalDate lastCouponDate) { this.lastCouponDate = lastCouponDate; }

    public BigDecimal getIssueSize() { return issueSize; }
    public void setIssueSize(BigDecimal issueSize) { this.issueSize = issueSize; }

    public BigDecimal getFaceValue() { return faceValue; }
    public void setFaceValue(BigDecimal faceValue) { this.faceValue = faceValue; }

    public BigDecimal getIssuePrice() { return issuePrice; }
    public void setIssuePrice(BigDecimal issuePrice) { this.issuePrice = issuePrice; }

    public CouponType getCouponType() { return couponType; }
    public void setCouponType(CouponType couponType) { this.couponType = couponType; }

    public BigDecimal getCoupon() { return coupon; }
    public void setCoupon(BigDecimal coupon) { this.coupon = coupon; }

    public int getCouponFrequency() { return couponFrequency; }
    public void setCouponFrequency(int couponFrequency) { this.couponFrequency = couponFrequency; }

    public DayCountConvention getDayCount() { return dayCount; }
    public void setDayCount(DayCountConvention dayCount) { this.dayCount = dayCount; }

    public String getFloatingIndex() { return floatingIndex; }
    public void setFloatingIndex(String floatingIndex) { this.floatingIndex = floatingIndex; }

    public BigDecimal getSpread() { return spread; }
    public void setSpread(BigDecimal spread) { this.spread = spread; }

    public List<ResetEntry> getResetSchedule() { return resetSchedule; }
    public void setResetSchedule(List<ResetEntry> resetSchedule) { this.resetSchedule = resetSchedule; }

    public List<CallEntry> getCallSchedule() { return callSchedule; }
    public void setCallSchedule(List<CallEntry> callSchedule) { this.callSchedule = callSchedule; }

    public List<PutEntry> getPutSchedule() { return putSchedule; }
    public void setPutSchedule(List<PutEntry> putSchedule) { this.putSchedule = putSchedule; }

    public List<SinkingFundEntry> getSinkingFundSchedule() { return sinkingFundSchedule; }
    public void setSinkingFundSchedule(List<SinkingFundEntry> sinkingFundSchedule) { this.sinkingFundSchedule = sinkingFundSchedule; }

    public String getMoodysRating() { return moodysRating; }
    public void setMoodysRating(String moodysRating) { this.moodysRating = moodysRating; }

    public String getSpRating() { return spRating; }
    public void setSpRating(String spRating) { this.spRating = spRating; }

    public String getFitchRating() { return fitchRating; }
    public void setFitchRating(String fitchRating) { this.fitchRating = fitchRating; }

    public boolean isSecured() { return secured; }
    public void setSecured(boolean secured) { this.secured = secured; }

    public String getSeniorityLevel() { return seniorityLevel; }
    public void setSeniorityLevel(String seniorityLevel) { this.seniorityLevel = seniorityLevel; }
}