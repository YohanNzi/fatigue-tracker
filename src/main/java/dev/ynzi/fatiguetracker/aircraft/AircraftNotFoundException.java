package dev.ynzi.fatiguetracker.aircraft;

/** Levée quand un {@link Aircraft} demandé par id n'existe pas. */
public class AircraftNotFoundException extends RuntimeException {

    public AircraftNotFoundException(Long id) {
        super("Aircraft introuvable pour l'id " + id);
    }
}
