package com.vetto.batch.config;

/**
 * Runtime knobs for the demo. Override via system properties, e.g.:
 * {@code -Dbatch.workers=8 -Dbatch.dbPermits=16 -Dbatch.policyCount=50000}
 */
public final class BatchConfig {

    private final int workerThreads;
    private final int dbPermits;
    private final int batchSize;
    private final int optimisticLockRetries;
    private final int policyCount;
    private final String jdbcUrl;
    private final String csvPath;

    public BatchConfig(
            int workerThreads,
            int dbPermits,
            int batchSize,
            int optimisticLockRetries,
            int policyCount,
            String jdbcUrl,
            String csvPath
    ) {
        this.workerThreads = workerThreads;
        this.dbPermits = dbPermits;
        this.batchSize = batchSize;
        this.optimisticLockRetries = optimisticLockRetries;
        this.policyCount = policyCount;
        this.jdbcUrl = jdbcUrl;
        this.csvPath = csvPath;
    }

    public static BatchConfig fromSystemProperties() {
        int cpus = Runtime.getRuntime().availableProcessors();
        return new BatchConfig(
                Integer.getInteger("batch.workers", Math.max(4, cpus)),
                Integer.getInteger("batch.dbPermits", Math.max(8, cpus * 2)),
                Integer.getInteger("batch.batchSize", 500),
                Integer.getInteger("batch.lockRetries", 5),
                Integer.getInteger("batch.policyCount", 20_000),
                System.getProperty("batch.jdbcUrl", "jdbc:h2:mem:reserves;DB_CLOSE_DELAY=-1"),
                System.getProperty("batch.csvPath", "")
        );
    }

    public int workerThreads() {
        return workerThreads;
    }

    public int dbPermits() {
        return dbPermits;
    }

    public int batchSize() {
        return batchSize;
    }

    public int optimisticLockRetries() {
        return optimisticLockRetries;
    }

    public int policyCount() {
        return policyCount;
    }

    public String jdbcUrl() {
        return jdbcUrl;
    }

    public String csvPath() {
        return csvPath;
    }

    @Override
    public String toString() {
        return "BatchConfig{workers=" + workerThreads
                + ", dbPermits=" + dbPermits
                + ", batchSize=" + batchSize
                + ", lockRetries=" + optimisticLockRetries
                + ", policyCount=" + policyCount
                + ", jdbcUrl=" + jdbcUrl
                + ", csvPath=" + csvPath + "}";
    }
}
