package dev.ynzi.fatiguetracker.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Vérifie l'émission/validation du JWT de façon isolée (aucun contexte Spring nécessaire) :
 * round-trip subject/rôle, rejet d'une signature étrangère, rejet d'un token expiré.
 */
class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "test-only-secret-key-not-used-anywhere-else-32bytes+".getBytes());

    @Test
    void generateToken_thenParse_roundTripsUsernameAndRole() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

        String token = jwtService.generateToken("demo.maint", "MAINT");
        Claims claims = jwtService.parseAndValidate(token);

        assertThat(jwtService.extractUsername(claims)).isEqualTo("demo.maint");
        assertThat(jwtService.extractRole(claims)).isEqualTo("MAINT");
    }

    @Test
    void parseAndValidate_withDifferentSigningKey_throws() {
        JwtService issuer = new JwtService(new JwtProperties(SECRET, 60));
        String otherSecret = Base64.getEncoder().encodeToString(
                "a-completely-different-secret-key-32-bytes-long!!".getBytes());
        JwtService verifier = new JwtService(new JwtProperties(otherSecret, 60));

        String token = issuer.generateToken("demo.maint", "MAINT");

        assertThatThrownBy(() -> verifier.parseAndValidate(token)).isInstanceOf(SignatureException.class);
    }

    @Test
    void parseAndValidate_withExpiredToken_throws() {
        JwtService jwtService = new JwtService(new JwtProperties(SECRET, 60));

        // Fabrique directement (hors JwtService) un token déjà expiré, signé avec la même clé.
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));
        Instant past = Instant.now().minusSeconds(3600);
        String expiredToken = Jwts.builder()
                .subject("demo.viewer")
                .claim("role", "VIEWER")
                .issuedAt(Date.from(past.minusSeconds(60)))
                .expiration(Date.from(past))
                .signWith(key)
                .compact();

        assertThatThrownBy(() -> jwtService.parseAndValidate(expiredToken)).isInstanceOf(ExpiredJwtException.class);
    }
}
