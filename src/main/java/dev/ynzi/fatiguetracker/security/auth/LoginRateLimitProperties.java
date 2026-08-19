package dev.ynzi.fatiguetracker.security.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de protection du point d'entrée de connexion contre les tentatives répétées.
 *
 * @param maxAttempts nombre maximal d'échecs autorisés dans la fenêtre
 * @param windowMinutes durée de la fenêtre glissante, en minutes
 */
@ConfigurationProperties(prefix = "security.login-rate-limit")
public record LoginRateLimitProperties(int maxAttempts, long windowMinutes) {
}
