package dev.ynzi.fatiguetracker.security.auth.dto;

/** Réponse de {@code POST /api/auth/login} : le JWT à fournir en {@code Authorization: Bearer <token>}. */
public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds, String role) {

    public static LoginResponse of(String accessToken, long expiresInSeconds, String role) {
        return new LoginResponse(accessToken, "Bearer", expiresInSeconds, role);
    }
}
