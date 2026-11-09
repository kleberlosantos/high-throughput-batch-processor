package com.vetto.batch.engine;

import com.vetto.batch.model.Policy;

import java.math.BigDecimal;

public final class PolicyValidator {

    public void validate(Policy policy) {
        if (policy.age() < 18 || policy.age() > 90) {
            throw new IllegalArgumentException("Invalid age for policy " + policy.id() + ": " + policy.age());
        }
        if (policy.faceAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Face amount must be positive for policy " + policy.id());
        }
        if (policy.productCode() == null || policy.productCode().isBlank()) {
            throw new IllegalArgumentException("Missing product code for policy " + policy.id());
        }
    }
}
