# high-throughput-batch-processor

Public demo of a **nightly reserve batch**: concurrent workers, DB backpressure, optimistic locking, bulk upserts, and a single-flight cache for static actuarial parameters.

This is a **simplified, synthetic** stand-in for a real insurance batch engine. No confidential data, schemas, or proprietary formulas.

## What it demonstrates

| Concern | Implementation |
|--------|----------------|
| High concurrency | Fixed worker pool + `CompletableFuture` per policy |
| Backpressure | Semaphore capping concurrent DB access (`batch.dbPermits`) |
| Optimistic locking | `UPDATE ... WHERE version = ?` with retry/reload |
| Cache stampede control | Single-flight load of actuarial rates (`ActuarialRateCache`) |
| Throughput visibility | Per-batch and final metrics (records/sec, retries, failures) |
| Bulk persistence | JDBC batch `MERGE` into `reserve_result` |

### Relation to the “wrong fix”

Raising JVM heap / widening the DB connection pool without a concurrency gate is the trap. Here, `batch.workers` can be high, but `batch.dbPermits` keeps the database from being flooded. Compare:

```bash
# Controlled (default idea)
java -Dbatch.workers=16 -Dbatch.dbPermits=16 -jar target/high-throughput-batch-processor-1.0.0.jar

# Naive oversubscription (more workers than DB permits still queues safely;
# remove the permit gate in code and you reintroduce latch/lock storms)
java -Dbatch.workers=64 -Dbatch.dbPermits=8 -jar target/high-throughput-batch-processor-1.0.0.jar
```

## Stack

- Java 17
- Maven
- H2 (embedded) — stand-in for Oracle in the real story
- SLF4J Simple for logs
- JUnit 5

## Project layout

```
src/main/java/com/vetto/batch/
  BatchApplication.java          # entry point
  config/BatchConfig.java        # -Dbatch.* knobs
  concurrency/ThrottledExecutor.java
  cache/ActuarialRateCache.java  # single-flight cache
  engine/                        # validate + calculate + orchestrate
  repository/                    # H2 schema, optimistic lock, bulk upsert
  metrics/BatchMetrics.java
  io/PolicyCsvLoader.java
sample-data/policies.csv         # optional small CSV fixture
```

## Quick start

```bash
mvn -q test
mvn -q package
java -jar target/high-throughput-batch-processor-1.0.0.jar
```

Default run seeds **20 000** synthetic policies into in-memory H2 and processes them.

### Useful system properties

| Property | Default | Meaning |
|----------|---------|---------|
| `batch.policyCount` | `20000` | Synthetic rows when no CSV is set |
| `batch.workers` | `#CPUs` (min 4) | Worker threads |
| `batch.dbPermits` | `#CPUs*2` (min 8) | Max concurrent DB operations |
| `batch.batchSize` | `500` | Page size |
| `batch.lockRetries` | `5` | Optimistic lock attempts |
| `batch.csvPath` | _(empty)_ | If set, load policies from CSV instead |
| `batch.jdbcUrl` | `jdbc:h2:mem:reserves;DB_CLOSE_DELAY=-1` | JDBC URL |

Examples:

```bash
# Larger run
java -Dbatch.policyCount=50000 -Dbatch.workers=8 -Dbatch.dbPermits=16 \
  -jar target/high-throughput-batch-processor-1.0.0.jar

# Tiny CSV fixture
java -Dbatch.csvPath=sample-data/policies.csv \
  -jar target/high-throughput-batch-processor-1.0.0.jar
```

## Sample log output

```
Starting with BatchConfig{workers=8, dbPermits=16, batchSize=500, ...}
Seeded 20000 synthetic policies ...
batch=1 size=500 took=412ms cumulative_ok=500 ... throughput=1213.6/s
...
DONE processed=20000 failed=0 optimisticRetries=0 elapsed=16.42s throughput=1218.0/s
```

## Design notes (honest scope)

- Formula is deliberately trivial (`faceAmount * factor`) — the point is **runtime mechanics**, not actuarial accuracy.
- Cache is in-process with the same stampede pattern you would use in front of Redis.
- H2 replaces Oracle; wait-event / AWR analysis from production is out of scope here.
- Not production software: no auth, no HA, no audit trail.

## License

MIT — use freely as a portfolio / interview artifact.
