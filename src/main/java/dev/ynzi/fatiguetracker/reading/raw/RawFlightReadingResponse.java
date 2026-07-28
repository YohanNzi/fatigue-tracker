package dev.ynzi.fatiguetracker.reading.raw;

import java.time.Instant;
import java.util.Map;

/** Représentation exposée d'un relevé brut (MongoDB, J5.5). */
public record RawFlightReadingResponse(
        String id,
        Long aircraftId,
        Instant recordedAt,
        int cycles,
        double maxLoadFactor,
        double flightHours,
        Instant receivedAt,
        String source,
        Map<String, Object> metadata
) {

    public static RawFlightReadingResponse from(RawFlightReading raw) {
        return new RawFlightReadingResponse(
                raw.getId(),
                raw.getAircraftId(),
                raw.getRecordedAt(),
                raw.getCycles(),
                raw.getMaxLoadFactor(),
                raw.getFlightHours(),
                raw.getReceivedAt(),
                raw.getSource(),
                raw.getMetadata()
        );
    }
}
