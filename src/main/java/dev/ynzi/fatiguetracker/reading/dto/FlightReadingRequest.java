package dev.ynzi.fatiguetracker.reading.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.Map;

/**
 * Corps de requête pour l'ingestion d'un relevé de vol sur un appareil donné.
 * <p>
 * {@code metadata} est optionnel : un dictionnaire d'attributs libres (capteur, firmware,
 * conditions…) conservé tel quel dans le store brut MongoDB (J5.5) sans contrainte de
 * schéma. Les colonnes normalisées ci-dessus alimentent PostgreSQL et le calcul de fatigue.
 */
public record FlightReadingRequest(

        @NotNull(message = "La date du relevé est obligatoire")
        Instant recordedAt,

        @PositiveOrZero(message = "Le nombre de cycles doit être positif ou nul")
        int cycles,

        double maxLoadFactor,

        @PositiveOrZero(message = "Les heures de vol doivent être positives ou nulles")
        double flightHours,

        Map<String, Object> metadata
) {

    /** Sans métadonnées (usage courant / rétro-compatible). */
    public FlightReadingRequest(Instant recordedAt, int cycles, double maxLoadFactor, double flightHours) {
        this(recordedAt, cycles, maxLoadFactor, flightHours, null);
    }
}
