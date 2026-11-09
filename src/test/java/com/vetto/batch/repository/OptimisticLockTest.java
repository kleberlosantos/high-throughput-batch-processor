package com.vetto.batch.repository;

import com.vetto.batch.model.Policy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptimisticLockTest {

    @Test
    void bumpVersionFailsWhenExpectedVersionIsStale() throws Exception {
        Database database = new Database("jdbc:h2:mem:lock_test;DB_CLOSE_DELAY=-1");
        database.migrate();
        PolicyRepository repository = new PolicyRepository(database);
        repository.upsertAll(List.of(
                new Policy(99L, "TERM10", 40, new BigDecimal("100000.00"), 0)
        ));

        assertTrue(repository.bumpVersion(99L, 0));
        assertFalse(repository.bumpVersion(99L, 0));
        assertTrue(repository.bumpVersion(99L, 1));
    }
}
