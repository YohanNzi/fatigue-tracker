package dev.ynzi.fatiguetracker.reading.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;

/** Corps de requête pour l'ingestion d'un relevé de vol sur un appareil donné. */
public record FlightReadingRequest(

        @NotNull(message = "La date du relevé est obligatoire")
        Instant recordedAt,

        @PositiveOrZero(message = "Le nombre de cycles doit être positif ou nul")
        int cycles,

        double maxLoadFactor,

        @PositiveOrZero(message = "Les heures de vol doivent être positives ou nulles")
        double flightHours
) {
}
