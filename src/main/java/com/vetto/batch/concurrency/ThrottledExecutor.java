package com.vetto.batch.concurrency;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Worker pool plus a semaphore that caps concurrent DB access (backpressure).
 * Upscaling the pool without this permit gate is the "obvious wrong fix"
 * described in the accompanying write-up.
 */
public final class ThrottledExecutor implements AutoCloseable {

    private final ExecutorService workers;
    private final Semaphore dbPermits;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ThrottledExecutor(int workerThreads, int dbPermits) {
        if (workerThreads < 1 || dbPermits < 1) {
            throw new IllegalArgumentException("workers and dbPermits must be >= 1");
        }
        this.workers = Executors.newFixedThreadPool(workerThreads, r -> {
            Thread t = new Thread(r);
            t.setName("batch-worker-" + t.getId());
            t.setDaemon(false);
            return t;
        });
        this.dbPermits = new Semaphore(dbPermits, true);
    }

    public ExecutorService workers() {
        return workers;
    }

    public <T> T withDbPermit(CheckedSupplier<T> action) throws Exception {
        Objects.requireNonNull(action, "action");
        dbPermits.acquire();
        try {
            return action.get();
        } finally {
            dbPermits.release();
        }
    }

    public void withDbPermit(CheckedRunnable action) throws Exception {
        withDbPermit(() -> {
            action.run();
            return null;
        });
    }

    public int availableDbPermits() {
        return dbPermits.availablePermits();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        workers.shutdown();
        try {
            if (!workers.awaitTermination(60, TimeUnit.SECONDS)) {
                workers.shutdownNow();
            }
        } catch (InterruptedException e) {
            workers.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @FunctionalInterface
    public interface CheckedSupplier<T> {
        T get() throws Exception;
    }

    @FunctionalInterface
    public interface CheckedRunnable {
        void run() throws Exception;
    }
}
