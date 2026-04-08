package com.bookie.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.Executors;

import static com.bookie.infra.Util.sleep;

@Service
public class PricingService {

    private final java.util.concurrent.ExecutorService executor =
            Executors.newVirtualThreadPerTaskExecutor();

    public BigDecimal calculateAccruedInterest(String cusip, BigDecimal quantity) {
        sleep(1000);
        return quantity.multiply(BigDecimal.valueOf(0.10));
    }
}