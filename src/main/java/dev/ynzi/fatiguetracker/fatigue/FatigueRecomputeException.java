package dev.ynzi.fatiguetracker.fatigue;

/** Levée quand le job Spring Batch de recalcul de fatigue ne peut pas être lancé. */
public class FatigueRecomputeException extends RuntimeException {

    public FatigueRecomputeException(String message, Throwable cause) {
        super(message, cause);
    }
}
