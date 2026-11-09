package com.vetto.batch.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class ActuarialRate {

    private final String productCode;
    private final int ageBand;
    private final BigDecimal factor;

    public ActuarialRate(String productCode, int ageBand, BigDecimal factor) {
        this.productCode = Objects.requireNonNull(productCode, "productCode");
        this.ageBand = ageBand;
        this.factor = Objects.requireNonNull(factor, "factor");
    }

    public String productCode() {
        return productCode;
    }

    public int ageBand() {
        return ageBand;
    }

    public BigDecimal factor() {
        return factor;
    }

    public String cacheKey() {
        return productCode + ":" + ageBand;
    }
}
