package com.bookie.domain.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

@Service
public class PricingService {

    private final java.util.concurrent.ExecutorService executor =
            Executors.newVirtualThreadPerTaskExecutor();

    public CompletableFuture<BigDecimal> calculateAccruedInterest(String cusip, BigDecimal quantity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return quantity.multiply(BigDecimal.valueOf(0.10));
        }, executor);
    }
}