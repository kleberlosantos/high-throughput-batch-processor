package com.vetto.batch.engine;

import com.vetto.batch.cache.ActuarialRateCache;
import com.vetto.batch.model.Policy;
import com.vetto.batch.model.ReserveResult;
import com.vetto.batch.repository.ActuarialRateRepository;
import com.vetto.batch.repository.Database;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReserveCalculatorTest {

    @Test
    void calculatesReserveUsingCachedRate() throws Exception {
        Database database = new Database("jdbc:h2:mem:calc_test;DB_CLOSE_DELAY=-1");
        database.migrate();
        ActuarialRateRepository rateRepository = new ActuarialRateRepository(database);
        rateRepository.seedDefaults();

        ReserveCalculator calculator = new ReserveCalculator(
                new ActuarialRateCache(rateRepository),
                new PolicyValidator()
        );

        Policy policy = new Policy(1L, "TERM10", 35, new BigDecimal("100000.00"), 0);
        ReserveResult result = calculator.calculate(policy);

        assertEquals(1L, result.policyId());
        assertTrue(result.reserveAmount().compareTo(BigDecimal.ZERO) > 0);
        assertEquals("TERM10:30", result.rateKey());
    }
}
