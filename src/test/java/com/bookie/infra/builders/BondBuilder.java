package com.bookie.infra.builders;

import com.bookie.domain.entity.Bond;
import com.bookie.domain.entity.BondType;
import com.bookie.domain.entity.CouponType;
import com.bookie.domain.entity.DayCountConvention;
import org.springframework.lang.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class BondBuilder {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2020, 1, 15);
    private static final LocalDate MATURITY_DATE = LocalDate.of(2030, 1, 15);

    @NonNull
    public static Bond aBond(String cusip) {
        Bond bond = new Bond();
        bond.setCusip(cusip);
        bond.setIsin("US" + cusip + "0");
        bond.setTicker(cusip);
        bond.setIssuerName("Test Issuer Corp.");
        bond.setDescription("Test Bond 5.0% 2030");
        bond.setBondType(BondType.CORPORATE);
        bond.setCurrency("USD");
        bond.setCountry("US");
        bond.setIssueDate(ISSUE_DATE);
        bond.setMaturityDate(MATURITY_DATE);
        bond.setFaceValue(new BigDecimal("1000"));
        bond.setIssuePrice(new BigDecimal("100.00"));
        bond.setIssueSize(new BigDecimal("1000000000"));
        bond.setCouponType(CouponType.FIXED);
        bond.setCoupon(new BigDecimal("5.00"));
        bond.setCouponFrequency(2);
        bond.setDayCount(DayCountConvention.THIRTY_360);
        return bond;
    }

}
