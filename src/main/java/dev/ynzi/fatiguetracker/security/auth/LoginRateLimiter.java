package dev.ynzi.fatiguetracker.security.auth;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/** Limiteur en mémoire des échecs de connexion, isolé par nom d'utilisateur. */
@Component
public class LoginRateLimiter {

    private final ConcurrentHashMap<String, ArrayDeque<Instant>> failuresByUsername = new ConcurrentHashMap<>();
    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;

    public LoginRateLimiter(LoginRateLimitProperties properties, Clock clock) {
        this.maxAttempts = properties.maxAttempts();
        this.window = Duration.ofMinutes(properties.windowMinutes());
        this.clock = clock;
    }

    public void checkNotBlocked(String username) {
        failuresByUsername.computeIfPresent(username, (ignored, failures) -> {
            removeExpired(failures);
            if (failures.isEmpty()) {
                return null;
            }
            if (failures.size() >= maxAttempts) {
                throw new TooManyLoginAttemptsException(username);
            }
            return failures;
        });
    }

    public void recordFailure(String username) {
        failuresByUsername.compute(username, (ignored, failures) -> {
            ArrayDeque<Instant> currentFailures = failures != null ? failures : new ArrayDeque<>();
            removeExpired(currentFailures);
            currentFailures.addLast(clock.instant());
            return currentFailures;
        });
    }

    public void recordSuccess(String username) {
        failuresByUsername.remove(username);
    }

    private void removeExpired(ArrayDeque<Instant> failures) {
        Instant threshold = clock.instant().minus(window);
        while (!failures.isEmpty() && !failures.getFirst().isAfter(threshold)) {
            failures.removeFirst();
        }
    }
}
