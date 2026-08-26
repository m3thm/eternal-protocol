package com.eternalprotocol.api.cache;

import com.eternalprotocol.api.dto.AthleteCodeValidationDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AthleteCodeCacheTest {

    private static final AthleteCodeValidationDto RISE10 =
            new AthleteCodeValidationDto(true, "RISE10", new BigDecimal("10.00"));

    @AfterEach
    void clearSharedSingleton() {
        // getInstance() is a genuine shared singleton — leaving entries in
        // it after a test would leak into whichever test runs next in this
        // JVM, exactly the risk called out in AthleteServiceTest.
        AthleteCodeCache.getInstance().clear();
    }

    @Test
    void getInstance_calledTwice_returnsTheExactSameInstance() {
        AthleteCodeCache first = AthleteCodeCache.getInstance();
        AthleteCodeCache second = AthleteCodeCache.getInstance();

        assertThat(first).isSameAs(second);
    }

    @Test
    void get_unknownCode_returnsEmpty() {
        AthleteCodeCache cache = AthleteCodeCache.getInstance();

        assertThat(cache.get("NEVER-CACHED")).isEmpty();
    }

    @Test
    void put_thenGet_returnsTheCachedResult() {
        AthleteCodeCache cache = AthleteCodeCache.getInstance();

        cache.put("RISE10", RISE10);

        assertThat(cache.get("RISE10")).contains(RISE10);
    }

    @Test
    void get_isCaseInsensitive() {
        AthleteCodeCache cache = AthleteCodeCache.getInstance();

        cache.put("rise10", RISE10);

        assertThat(cache.get("RISE10")).contains(RISE10);
        assertThat(cache.get("Rise10")).contains(RISE10);
    }

    @Test
    void evict_removesOnlyTheGivenCode() {
        AthleteCodeCache cache = AthleteCodeCache.getInstance();
        AthleteCodeValidationDto other = new AthleteCodeValidationDto(true, "SPEED5", new BigDecimal("5.00"));
        cache.put("RISE10", RISE10);
        cache.put("SPEED5", other);

        cache.evict("RISE10");

        assertThat(cache.get("RISE10")).isEmpty();
        assertThat(cache.get("SPEED5")).contains(other); // untouched
    }

    @Test
    void clear_removesEveryEntry() {
        AthleteCodeCache cache = AthleteCodeCache.getInstance();
        cache.put("RISE10", RISE10);
        cache.put("SPEED5", new AthleteCodeValidationDto(true, "SPEED5", new BigDecimal("5.00")));

        cache.clear();

        assertThat(cache.size()).isZero();
    }

    @Test
    void get_afterTtlExpires_returnsEmptyAndForgetsTheEntry() throws InterruptedException {
        // Uses the package-private, short-TTL constructor directly (not
        // getInstance()) so this test doesn't need a real 5-minute wait —
        // see AthleteCodeCache's JavaDoc on why that constructor exists.
        AthleteCodeCache shortLivedCache = new AthleteCodeCache(20); // 20ms TTL
        shortLivedCache.put("RISE10", RISE10);
        assertThat(shortLivedCache.get("RISE10")).contains(RISE10); // fresh immediately after put

        Thread.sleep(100); // comfortably past the 20ms TTL

        assertThat(shortLivedCache.get("RISE10")).isEmpty();
        assertThat(shortLivedCache.size()).isZero(); // expired entry is evicted on read, not just hidden
    }

    @Test
    void differentCacheInstances_doNotShareEntries() {
        // Confirms the package-private test constructor genuinely creates
        // an independent object rather than touching the shared singleton.
        AthleteCodeCache independent = new AthleteCodeCache(60_000);

        independent.put("RISE10", RISE10);

        assertThat(AthleteCodeCache.getInstance().get("RISE10")).isEmpty();
    }
}
