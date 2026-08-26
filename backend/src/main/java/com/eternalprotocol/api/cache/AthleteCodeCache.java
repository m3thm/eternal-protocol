package com.eternalprotocol.api.cache;

import com.eternalprotocol.api.dto.AthleteCodeValidationDto;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single, shared, in-memory cache of recent athlete discount-code
 * lookups (Singleton pattern), backing the checkout page's live discount
 * preview as a customer types a code.
 * <p>
 * Hand-written singleton (private constructor + {@link #getInstance()})
 * rather than a Spring bean, using lazy, double-checked-locking
 * initialisation so {@link #instance} is only created on first use.
 * <p>
 * Only {@code AthleteService.validateCode} reads from this cache. Checkout
 * itself always re-queries the database, so a stale cached result can
 * never change what a customer is actually charged.
 */
public final class AthleteCodeCache {

    /** How long a cached result stays fresh before a lookup is treated as a miss. */
    private static final long DEFAULT_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes

    private static volatile AthleteCodeCache instance;

    private final Map<String, CacheEntry> entriesByCode = new ConcurrentHashMap<>();
    private final long ttlMillis;

    private AthleteCodeCache() {
        this(DEFAULT_TTL_MILLIS);
    }

    /**
     * Package-private constructor for tests, so expiry can be tested with
     * a short TTL instead of a real 5-minute wait. Production code must
     * always go through {@link #getInstance()}.
     *
     * @param ttlMillis how long a cached entry stays fresh, in milliseconds
     */
    AthleteCodeCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /**
     * Returns the application's single shared cache instance, creating it
     * on the first call.
     *
     * @return the shared instance
     */
    public static AthleteCodeCache getInstance() {
        AthleteCodeCache result = instance;
        if (result == null) {
            synchronized (AthleteCodeCache.class) {
                result = instance;
                if (result == null) {
                    instance = result = new AthleteCodeCache();
                }
            }
        }
        return result;
    }

    /**
     * Looks up a code's cached validation result, if one is present and
     * still fresh.
     *
     * @param code the athlete code to look up, matched case-insensitively
     * @return the cached result, or empty if not cached or expired
     */
    public Optional<AthleteCodeValidationDto> get(String code) {
        if (code == null) {
            return Optional.empty();
        }
        String key = code.toUpperCase();
        CacheEntry entry = entriesByCode.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired(ttlMillis)) {
            entriesByCode.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.result);
    }

    /**
     * Caches a code's validation result, timestamped as of right now.
     *
     * @param code   the athlete code the result belongs to
     * @param result the result to cache
     */
    public void put(String code, AthleteCodeValidationDto result) {
        if (code == null) {
            return;
        }
        entriesByCode.put(code.toUpperCase(), new CacheEntry(result, System.currentTimeMillis()));
    }

    /**
     * Removes one code's cached entry, if present. Called whenever an
     * admin changes an athlete's commission rate or active status, so the
     * live checkout preview can't keep showing a stale discount.
     *
     * @param code the athlete code to remove from the cache
     */
    public void evict(String code) {
        if (code == null) {
            return;
        }
        entriesByCode.remove(code.toUpperCase());
    }

    /** Clears every cached entry. Mainly for tests to start from a known-empty cache. */
    public void clear() {
        entriesByCode.clear();
    }

    /**
     * @return how many codes currently have a cached entry (expired or not)
     */
    public int size() {
        return entriesByCode.size();
    }

    /** One cached lookup result, timestamped so it can expire. */
    private static final class CacheEntry {
        private final AthleteCodeValidationDto result;
        private final long cachedAtMillis;

        private CacheEntry(AthleteCodeValidationDto result, long cachedAtMillis) {
            this.result = result;
            this.cachedAtMillis = cachedAtMillis;
        }

        private boolean isExpired(long ttlMillis) {
            return System.currentTimeMillis() - cachedAtMillis > ttlMillis;
        }
    }
}
