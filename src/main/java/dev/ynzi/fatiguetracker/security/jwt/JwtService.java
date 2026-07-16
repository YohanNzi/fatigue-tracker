package dev.ynzi.fatiguetracker.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Émission et validation des JWT stateless (HS256, J3). Le token porte le nom
 * d'utilisateur en subject et le rôle applicatif ({@link dev.ynzi.fatiguetracker.security.user.Role})
 * en claim {@code role} : le filtre {@link JwtAuthenticationFilter} n'a donc jamais besoin
 * de retourner en base pour reconstruire les autorités d'une requête authentifiée.
 */
@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";

    private final SecretKey signingKey;
    private final Duration expiration;

    public JwtService(JwtProperties properties) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(properties.secret()));
        this.expiration = Duration.ofMinutes(properties.expirationMinutes());
    }

    public String generateToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    public long expirationSeconds() {
        return expiration.toSeconds();
    }

    /**
     * Valide la signature et l'expiration puis retourne les claims.
     *
     * @throws JwtException si le token est invalide, malformé, expiré ou mal signé
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(Claims claims) {
        return claims.getSubject();
    }

    public String extractRole(Claims claims) {
        return claims.get(ROLE_CLAIM, String.class);
    }
}
