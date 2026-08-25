package com.eternalprotocol.api.cache;

import com.eternalprotocol.api.dto.AthleteCodeValidationDto;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A single, shared, in-memory cache of recent athlete discount-code
 * lookups — a hand-written example of the Singleton design pattern.
 * <p>
 * <b>Why a Singleton, and why here specifically:</b> the checkout page
 * calls {@code GET /api/athletes/validate-code/{code}} on every keystroke
 * to show a live discount preview as a customer types an athlete's code
 * (see {@link com.eternalprotocol.api.controller.AthleteController}), so
 * the same one or two codes tend to get looked up repeatedly in a short
 * window. A single cache shared across the whole running application —
 * not one per request, not one per service instance — is exactly the
 * "ensure one instance, with one global point of access to it" problem
 * the Singleton pattern solves.
 * <p>
 * <b>Deliberately not a {@code @Service}/{@code @Component}:</b> this
 * class implements the classic pattern by hand — a private constructor
 * plus a static {@link #getInstance()} — instead of leaning on Spring's
 * singleton-scoped beans. Contrast with
 * {@link com.eternalprotocol.api.command.CartInvoker}, which is a
 * {@code @Component} and is therefore also a single shared instance, but
 * gets that behaviour from framework configuration rather than from this
 * pattern.
 * <p>
 * <b>Which flavour of Singleton this is:</b> lazy, thread-safe
 * initialisation via double-checked locking — {@link #instance} is only
 * created the first time {@link #getInstance()} is actually called (lazy,
 * unlike eager instantiation, which would create it the moment the class
 * loads whether it's ever used or not), and the {@code volatile} field
 * plus the {@code synchronized} block make that lazy creation safe if two
 * requests reach {@link #getInstance()} at the exact same time — without
 * paying the cost of synchronizing on every single call once the instance
 * already exists (the outer, unsynchronized null-check handles that). An
 * {@code enum}-based Singleton would sidestep needing to reason about
 * thread-safety at all, at the cost of being unable to be a normal class
 * with fields like {@link #entriesByCode} initialised the usual way.
 * <p>
 * <b>Correctness note:</b> only {@link com.eternalprotocol.api.service.AthleteService#validateCode}
 * (the live-preview endpoint) reads from this cache. The actual discount
 * applied at checkout, in {@code OrderService.createOrder}, always queries
 * the database directly and never consults this cache — a stale cached
 * "valid" result must never be able to change what a customer is actually
 * charged, only how quickly the preview updates while they're typing.
 */
public final class AthleteCodeCache {

    /** How long a cached result stays fresh before a lookup is treated as a miss. */
    private static final long DEFAULT_TTL_MILLIS = 5 * 60 * 1000; // 5 minutes

    /**
     * {@code volatile} so a thread that sees a non-null value here is
     * guaranteed to see a fully-constructed {@code AthleteCodeCache}, not
     * a half-initialised one — the other half of what makes double-checked
     * locking safe on the JVM's memory model.
     */
    private static volatile AthleteCodeCache instance;

    private final Map<String, CacheEntry> entriesByCode = new ConcurrentHashMap<>();
    private final long ttlMillis;

    /** Real construction path — always a fixed, five-minute TTL. */
    private AthleteCodeCache() {
        this(DEFAULT_TTL_MILLIS);
    }

    /**
     * Package-private constructor used only by this package's tests, so
     * expiry behaviour can be tested with a short TTL instead of a real
     * {@code Thread.sleep(5 minutes)}. Production code must always go
     * through {@link #getInstance()} — this constructor deliberately
     * doesn't touch {@link #instance}, so calling it never disturbs the
     * single shared instance the rest of the application sees.
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
     * Removes one code's cached entry, if present. Called by
     * {@link com.eternalprotocol.api.service.AthleteService#updateAthlete}
     * whenever an admin changes an athlete's commission rate or active
     * status, so the live checkout preview can never keep showing a
     * discount rate that no longer applies.
     *
     * @param code the athlete code to remove from the cache
     */
    public void evict(String code) {
        if (code == null) {
            return;
        }
        entriesByCode.remove(code.toUpperCase());
    }

    /**
     * Clears every cached entry. Exists mainly so tests can start from a
     * known-empty cache despite this being a shared, static singleton —
     * see the JavaDoc on {@code AthleteServiceTest} for why that's needed.
     */
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
