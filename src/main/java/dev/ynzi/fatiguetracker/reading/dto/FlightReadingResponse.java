package dev.ynzi.fatiguetracker.reading.dto;

import dev.ynzi.fatiguetracker.reading.FlightReading;

import java.time.Instant;

/** Représentation exposée en sortie d'API — ne jamais renvoyer l'entité JPA directement. */
public record FlightReadingResponse(
        Long id,
        Long aircraftId,
        Instant recordedAt,
        int cycles,
        double maxLoadFactor,
        double flightHours
) {

    public static FlightReadingResponse from(FlightReading reading) {
        return new FlightReadingResponse(
                reading.getId(),
                reading.getAircraft().getId(),
                reading.getRecordedAt(),
                reading.getCycles(),
                reading.getMaxLoadFactor(),
                reading.getFlightHours()
        );
    }
}
