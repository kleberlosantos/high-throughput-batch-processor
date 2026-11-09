package com.vetto.batch.model;

import java.math.BigDecimal;
import java.util.Objects;

public final class Policy {

    private final long id;
    private final String productCode;
    private final int age;
    private final BigDecimal faceAmount;
    private final int version;

    public Policy(long id, String productCode, int age, BigDecimal faceAmount, int version) {
        this.id = id;
        this.productCode = Objects.requireNonNull(productCode, "productCode");
        this.age = age;
        this.faceAmount = Objects.requireNonNull(faceAmount, "faceAmount");
        this.version = version;
    }

    public long id() {
        return id;
    }

    public String productCode() {
        return productCode;
    }

    public int age() {
        return age;
    }

    public BigDecimal faceAmount() {
        return faceAmount;
    }

    public int version() {
        return version;
    }

    public Policy withVersion(int newVersion) {
        return new Policy(id, productCode, age, faceAmount, newVersion);
    }
}
