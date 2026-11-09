package com.vetto.batch.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class ReserveResult {

    private final long policyId;
    private final BigDecimal reserveAmount;
    private final String rateKey;
    private final long durationMs;

    public ReserveResult(long policyId, BigDecimal reserveAmount, String rateKey, long durationMs) {
        this.policyId = policyId;
        this.reserveAmount = Objects.requireNonNull(reserveAmount, "reserveAmount");
        this.rateKey = Objects.requireNonNull(rateKey, "rateKey");
        this.durationMs = durationMs;
    }

    public long policyId() {
        return policyId;
    }

    public BigDecimal reserveAmount() {
        return reserveAmount;
    }

    public String rateKey() {
        return rateKey;
    }

    public long durationMs() {
        return durationMs;
    }
}
