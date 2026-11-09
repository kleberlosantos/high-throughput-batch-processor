package com.vetto.batch.cache;

import com.vetto.batch.model.ActuarialRate;
import com.vetto.batch.repository.ActuarialRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory cache with single-flight loading to avoid cache stampede:
 * when a key expires/misses, only one thread loads from the DB while others wait.
 * <p>
 * Same idea as a Redis-backed cache for static actuarial parameters, without
 * requiring infrastructure for the public demo.
 */
public final class ActuarialRateCache {

    private static final Logger log = LoggerFactory.getLogger(ActuarialRateCache.class);

    private final ActuarialRateRepository repository;
    private final ConcurrentMap<String, ActuarialRate> values = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<ActuarialRate>> inflight = new ConcurrentHashMap<>();

    public ActuarialRateCache(ActuarialRateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public ActuarialRate get(String productCode, int ageBand) {
        String key = productCode + ":" + ageBand;
        ActuarialRate cached = values.get(key);
        if (cached != null) {
            return cached;
        }

        CompletableFuture<ActuarialRate> future = inflight.computeIfAbsent(key, k ->
                CompletableFuture.supplyAsync(() -> {
                    log.debug("Cache miss for {}; loading from DB (single-flight)", k);
                    ActuarialRate loaded = repository.findRequired(productCode, ageBand);
                    values.put(k, loaded);
                    return loaded;
                })
        );

        try {
            return future.join();
        } finally {
            inflight.remove(key, future);
        }
    }

    public int size() {
        return values.size();
    }
}
