package com.vetto.batch;

import com.vetto.batch.cache.ActuarialRateCache;
import com.vetto.batch.concurrency.ThrottledExecutor;
import com.vetto.batch.config.BatchConfig;
import com.vetto.batch.engine.BatchProcessor;
import com.vetto.batch.engine.PolicyValidator;
import com.vetto.batch.engine.ReserveCalculator;
import com.vetto.batch.io.PolicyCsvLoader;
import com.vetto.batch.metrics.BatchMetrics;
import com.vetto.batch.model.Policy;
import com.vetto.batch.repository.ActuarialRateRepository;
import com.vetto.batch.repository.Database;
import com.vetto.batch.repository.PolicyRepository;
import com.vetto.batch.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Entry point for the public demo of a high-throughput reserve batch.
 *
 * <pre>
 * mvn -q package
 * java -jar target/high-throughput-batch-processor-1.0.0.jar
 * java -Dbatch.policyCount=50000 -Dbatch.workers=8 -Dbatch.dbPermits=16 -jar target/...jar
 * </pre>
 */
public final class BatchApplication {

    private static final Logger log = LoggerFactory.getLogger(BatchApplication.class);

    private BatchApplication() {
    }

    public static void main(String[] args) throws Exception {
        BatchConfig config = BatchConfig.fromSystemProperties();
        log.info("Starting with {}", config);

        try (Database database = new Database(config.jdbcUrl());
             ThrottledExecutor executor = new ThrottledExecutor(config.workerThreads(), config.dbPermits())) {

            database.migrate();

            ActuarialRateRepository rateRepository = new ActuarialRateRepository(database);
            PolicyRepository policyRepository = new PolicyRepository(database);
            ResultRepository resultRepository = new ResultRepository(database);

            rateRepository.seedDefaults();
            loadPolicies(config, policyRepository);

            ActuarialRateCache rateCache = new ActuarialRateCache(rateRepository);
            ReserveCalculator calculator = new ReserveCalculator(rateCache, new PolicyValidator());
            BatchMetrics metrics = new BatchMetrics();

            BatchProcessor processor = new BatchProcessor(
                    config,
                    policyRepository,
                    resultRepository,
                    calculator,
                    executor,
                    metrics
            );

            processor.run();
            log.info("Actuarial cache entries loaded={}", rateCache.size());
            log.info("DB permits remaining at idle={}", executor.availableDbPermits());
        }
    }

    private static void loadPolicies(BatchConfig config, PolicyRepository policyRepository) throws Exception {
        if (config.csvPath() != null && !config.csvPath().isBlank()) {
            Path csv = Path.of(config.csvPath());
            if (!Files.exists(csv)) {
                throw new IllegalArgumentException("CSV not found: " + csv.toAbsolutePath());
            }
            List<Policy> policies = new PolicyCsvLoader().load(csv);
            policyRepository.upsertAll(policies);
            log.info("Loaded {} policies from {}", policies.size(), csv.toAbsolutePath());
            return;
        }
        policyRepository.seedSynthetic(config.policyCount());
        log.info("Seeded {} synthetic policies (pass -Dbatch.csvPath=... to use CSV)", config.policyCount());
    }
}
