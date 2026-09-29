package dev.ynzi.fatiguetracker.common;

/**
 * Violation d'un invariant du modèle (relevé physiquement impossible, heures de vol
 * négatives…). Levée par les entités elles-mêmes, indépendamment de la validation
 * d'entrée HTTP : un objet du domaine invalide ne doit pas pouvoir exister, quel que
 * soit le chemin de création (API, seed, batch, test). Traduite en 400 par
 * {@link GlobalExceptionHandler}.
 */
public class DomainRuleViolationException extends RuntimeException {

    public DomainRuleViolationException(String message) {
        super(message);
    }

    /** Rejette une valeur absente. */
    public static <T> T requirePresent(T value, String message) {
        if (value == null) {
            throw new DomainRuleViolationException(message);
        }
        return value;
    }

    /** Rejette un texte absent ou blanc. */
    public static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new DomainRuleViolationException(message);
        }
        return value;
    }

    /** Rejette une valeur négative ou non finie (NaN, ±∞). */
    public static double requirePositiveOrZero(double value, String message) {
        if (!Double.isFinite(value) || value < 0) {
            throw new DomainRuleViolationException(message);
        }
        return value;
    }

    /** Rejette une valeur nulle, négative ou non finie (NaN, ±∞). */
    public static double requireStrictlyPositive(double value, String message) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new DomainRuleViolationException(message);
        }
        return value;
    }
}
