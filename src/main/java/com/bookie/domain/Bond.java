package com.bookie.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class Bond {

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
    private List<ResetEntry> resetSchedule;
    private List<CallEntry> callSchedule;
    private List<PutEntry> putSchedule;
    private List<SinkingFundEntry> sinkingFundSchedule;

    private String moodysRating;
    private String spRating;
    private String fitchRating;
    private boolean secured;
    private String seniorityLevel;

    public Bond() {}

    public record ResetEntry(LocalDate resetDate, BigDecimal newRate) {}

    public record CallEntry(LocalDate callDate, BigDecimal callPrice) {}

    public record PutEntry(LocalDate putDate, BigDecimal putPrice) {}

    public record SinkingFundEntry(LocalDate sinkDate, BigDecimal amount) {}

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