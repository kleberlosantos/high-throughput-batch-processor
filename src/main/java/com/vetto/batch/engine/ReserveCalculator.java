package com.vetto.batch.engine;

import com.vetto.batch.cache.ActuarialRateCache;
import com.vetto.batch.model.ActuarialRate;
import com.vetto.batch.model.Policy;
import com.vetto.batch.model.ReserveResult;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Simplified reserve formula for the demo:
 * {@code reserve = faceAmount * actuarialFactor}.
 * Static rate lookups go through the cache to avoid hammering the DB.
 */
public final class ReserveCalculator {

    private final ActuarialRateCache rateCache;
    private final PolicyValidator validator;

    public ReserveCalculator(ActuarialRateCache rateCache, PolicyValidator validator) {
        this.rateCache = rateCache;
        this.validator = validator;
    }

    public ReserveResult calculate(Policy policy) {
        long started = System.nanoTime();
        validator.validate(policy);

        int ageBand = (policy.age() / 10) * 10;
        ActuarialRate rate = rateCache.get(policy.productCode(), ageBand);
        BigDecimal reserve = policy.faceAmount()
                .multiply(rate.factor())
                .setScale(2, RoundingMode.HALF_UP);

        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        return new ReserveResult(policy.id(), reserve, rate.cacheKey(), durationMs);
    }
}
