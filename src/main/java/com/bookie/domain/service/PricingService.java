package com.bookie.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static com.bookie.infra.Util.sleep;

@Service
public class PricingService {

    private long calculationDuration = 1000L;

    public PricingService() {
    }

    public PricingService(long calculationDuration) {
        this.calculationDuration = calculationDuration;
    }

    @SuppressWarnings("unused")
    public BigDecimal calculateAccruedInterest(String cusip, BigDecimal quantity) {
        sleep(this.calculationDuration);
        return quantity.multiply(BigDecimal.valueOf(0.10));
    }
}