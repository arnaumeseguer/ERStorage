package com.erstorage.erstorageapi.demo;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DemoRateLimiterTest {

    private final DemoProperties properties = new DemoProperties(
            true, Duration.ofHours(1), DataSize.ofMegabytes(20), DataSize.ofMegabytes(5), 30, 2);

    @Test
    void allowsUpToTheLimitThenRefuses() {
        DemoRateLimiter limiter = new DemoRateLimiter(properties);
        Instant now = Instant.parse("2026-11-25T10:00:00Z");

        assertThat(limiter.tryAcquire("1.2.3.4", now)).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4", now.plusSeconds(60))).isTrue();
        assertThat(limiter.tryAcquire("1.2.3.4", now.plusSeconds(120))).isFalse();
    }

    @Test
    void eachClientHasItsOwnLimit() {
        DemoRateLimiter limiter = new DemoRateLimiter(properties);
        Instant now = Instant.parse("2026-11-25T10:00:00Z");

        limiter.tryAcquire("1.2.3.4", now);
        limiter.tryAcquire("1.2.3.4", now);

        assertThat(limiter.tryAcquire("5.6.7.8", now)).isTrue();
    }

    @Test
    void allowsAgainAfterTheWindowHasPassed() {
        DemoRateLimiter limiter = new DemoRateLimiter(properties);
        Instant now = Instant.parse("2026-11-25T10:00:00Z");

        limiter.tryAcquire("1.2.3.4", now);
        limiter.tryAcquire("1.2.3.4", now);

        assertThat(limiter.tryAcquire("1.2.3.4", now.plus(Duration.ofMinutes(61)))).isTrue();
    }
}