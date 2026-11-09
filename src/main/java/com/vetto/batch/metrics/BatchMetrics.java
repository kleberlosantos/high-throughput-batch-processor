package com.vetto.batch.metrics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

public final class BatchMetrics {

    private static final Logger log = LoggerFactory.getLogger(BatchMetrics.class);

    private final LongAdder processed = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder optimisticRetries = new LongAdder();
    private final AtomicLong wallStartNanos = new AtomicLong();
    private final AtomicLong wallEndNanos = new AtomicLong();

    public void markStart() {
        wallStartNanos.set(System.nanoTime());
    }

    public void markEnd() {
        wallEndNanos.set(System.nanoTime());
    }

    public void recordSuccess(int count) {
        processed.add(count);
    }

    public void recordFailure(int count) {
        failed.add(count);
    }

    public void recordOptimisticRetry() {
        optimisticRetries.increment();
    }

    public long processed() {
        return processed.sum();
    }

    public long failed() {
        return failed.sum();
    }

    public long optimisticRetries() {
        return optimisticRetries.sum();
    }

    public double elapsedSeconds() {
        long start = wallStartNanos.get();
        long end = wallEndNanos.get() == 0 ? System.nanoTime() : wallEndNanos.get();
        return Math.max(0.001, (end - start) / 1_000_000_000.0);
    }

    public double throughputPerSecond() {
        return processed() / elapsedSeconds();
    }

    public void logBatch(int batchIndex, int batchSize, long batchMs) {
        log.info(
                "batch={} size={} took={}ms cumulative_ok={} cumulative_fail={} retries={} throughput={}/s",
                batchIndex,
                batchSize,
                batchMs,
                processed(),
                failed(),
                optimisticRetries(),
                String.format(Locale.US, "%.1f", throughputPerSecond())
        );
    }

    public void logSummary() {
        log.info(
                "DONE processed={} failed={} optimisticRetries={} elapsed={}s throughput={}/s",
                processed(),
                failed(),
                optimisticRetries(),
                String.format(Locale.US, "%.2f", elapsedSeconds()),
                String.format(Locale.US, "%.1f", throughputPerSecond())
        );
    }
}
