package dev.ynzi.fatiguetracker.security.user;

/**
 * Rôles applicatifs (J3).
 * <p>
 * {@code VIEWER} n'apporte aujourd'hui aucun droit supplémentaire par rapport à un
 * appelant anonyme (toute la lecture de l'API est déjà publique, voir SecurityConfig) :
 * il est conservé pour la cohérence du modèle et une éventuelle évolution future
 * (ex. lecture de données sensibles réservées aux comptes authentifiés). {@code MAINT}
 * est le seul rôle habilité à déclencher les écritures (CRUD aircraft/readings) et le
 * recalcul de fatigue.
 */
public enum Role {
    VIEWER,
    MAINT
}
