package dev.ynzi.fatiguetracker.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres du JWT stateless (J3, voir {@link JwtService} et application.yml).
 *
 * @param secret          clé de signature HS256, encodée base64 (au moins 256 bits une
 *                        fois décodée). <b>Fournie via la variable d'environnement
 *                        {@code JWT_SECRET} en dehors de dev</b> — la valeur par défaut
 *                        d'application.yml est explicitement marquée "dev only" et ne
 *                        doit jamais servir en production.
 * @param expirationMinutes durée de validité d'un token émis
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, long expirationMinutes) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("security.jwt.secret est requis");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("security.jwt.expiration-minutes doit être strictement positif");
        }
    }
}
