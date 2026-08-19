package dev.ynzi.fatiguetracker.security.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private MutableClock clock;
    private LoginRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-08-19T10:00:00Z"));
        rateLimiter = new LoginRateLimiter(new LoginRateLimitProperties(3, 15), clock);
    }

    @Test
    void checkNotBlocked_afterMaxAttemptsFailures_blocksLogin() {
        recordFailures(3);

        assertThatThrownBy(() -> rateLimiter.checkNotBlocked("demo.maint"))
                .isInstanceOf(TooManyLoginAttemptsException.class);
    }

    @Test
    void checkNotBlocked_afterWindowExpiration_unblocksLogin() {
        recordFailures(3);
        clock.advance(Duration.ofMinutes(15));

        assertThatNoException().isThrownBy(() -> rateLimiter.checkNotBlocked("demo.maint"));
    }

    @Test
    void recordSuccess_resetsFailures() {
        recordFailures(3);
        rateLimiter.recordSuccess("demo.maint");

        assertThatNoException().isThrownBy(() -> rateLimiter.checkNotBlocked("demo.maint"));
    }

    private void recordFailures(int count) {
        for (int attempt = 0; attempt < count; attempt++) {
            rateLimiter.recordFailure("demo.maint");
        }
    }

    /** Horloge fondée sur un instant fixe, avançable explicitement par les tests. */
    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
