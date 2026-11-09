package com.vetto.batch.engine;

import com.vetto.batch.concurrency.ThrottledExecutor;
import com.vetto.batch.config.BatchConfig;
import com.vetto.batch.metrics.BatchMetrics;
import com.vetto.batch.model.Policy;
import com.vetto.batch.model.ReserveResult;
import com.vetto.batch.repository.PolicyRepository;
import com.vetto.batch.repository.ResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Reads policies in pages, calculates reserves concurrently, persists with
 * optimistic locking + bulk upsert, and applies DB backpressure via permits.
 */
public final class BatchProcessor {

    private static final Logger log = LoggerFactory.getLogger(BatchProcessor.class);

    private final BatchConfig config;
    private final PolicyRepository policyRepository;
    private final ResultRepository resultRepository;
    private final ReserveCalculator calculator;
    private final ThrottledExecutor executor;
    private final BatchMetrics metrics;

    public BatchProcessor(
            BatchConfig config,
            PolicyRepository policyRepository,
            ResultRepository resultRepository,
            ReserveCalculator calculator,
            ThrottledExecutor executor,
            BatchMetrics metrics
    ) {
        this.config = config;
        this.policyRepository = policyRepository;
        this.resultRepository = resultRepository;
        this.calculator = calculator;
        this.executor = executor;
        this.metrics = metrics;
    }

    public void run() throws Exception {
        metrics.markStart();
        long afterId = 0L;
        int batchIndex = 0;

        while (true) {
            List<Policy> page = policyRepository.findPage(afterId, config.batchSize());
            if (page.isEmpty()) {
                break;
            }
            batchIndex++;
            long batchStarted = System.currentTimeMillis();
            processPage(page);
            long batchMs = System.currentTimeMillis() - batchStarted;
            metrics.logBatch(batchIndex, page.size(), batchMs);
            afterId = page.get(page.size() - 1).id();
        }

        metrics.markEnd();
        metrics.logSummary();
    }

    private void processPage(List<Policy> page) {
        List<CompletableFuture<ReserveResult>> futures = new ArrayList<>(page.size());

        for (Policy policy : page) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return processOne(policy);
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            }, executor.workers()));
        }

        List<ReserveResult> successes = new ArrayList<>();
        int failures = 0;
        for (CompletableFuture<ReserveResult> future : futures) {
            try {
                successes.add(future.join());
            } catch (CompletionException ex) {
                failures++;
                log.warn("Policy failed: {}", rootMessage(ex));
            }
        }

        try {
            executor.withDbPermit(() -> resultRepository.bulkUpsert(successes));
            metrics.recordSuccess(successes.size());
            metrics.recordFailure(failures);
        } catch (Exception e) {
            metrics.recordFailure(page.size());
            throw new IllegalStateException("Bulk upsert failed for page starting at " + page.get(0).id(), e);
        }
    }

    private ReserveResult processOne(Policy policy) throws Exception {
        int attempts = 0;
        Policy current = policy;

        while (true) {
            attempts++;
            final Policy snapshot = current;
            ReserveResult result = calculator.calculate(snapshot);

            boolean updated = executor.withDbPermit(
                    () -> policyRepository.bumpVersion(snapshot.id(), snapshot.version())
            );

            if (updated) {
                return result;
            }

            metrics.recordOptimisticRetry();
            if (attempts >= config.optimisticLockRetries()) {
                throw new IllegalStateException(
                        "Optimistic lock retries exhausted for policy " + snapshot.id()
                );
            }

            Policy refreshed = executor.withDbPermit(() -> policyRepository.findById(snapshot.id()));
            if (refreshed == null) {
                throw new IllegalStateException("Policy disappeared: " + snapshot.id());
            }
            current = refreshed;
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
