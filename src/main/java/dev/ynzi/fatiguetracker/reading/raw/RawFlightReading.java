package dev.ynzi.fatiguetracker.reading.raw;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

/**
 * Relevé de vol <b>brut</b> tel que reçu, stocké dans MongoDB (J5.5).
 * <p>
 * Persistance polyglotte : la version normalisée/relationnelle vit dans PostgreSQL
 * ({@link dev.ynzi.fatiguetracker.reading.FlightReading}, source de vérité du calcul de
 * fatigue) ; ce document Mongo est une <b>zone d'atterrissage append-only</b> pour la
 * télémétrie brute — volumineuse, à schéma souple. Le champ {@code metadata} accueille
 * des attributs hétérogènes selon la source/le capteur, sans migration de schéma.
 */
@Document(collection = "raw_flight_readings")
public class RawFlightReading {

    @Id
    private String id;

    @Indexed
    private Long aircraftId;

    private Instant recordedAt;
    private int cycles;
    private double maxLoadFactor;
    private double flightHours;
    private Instant receivedAt;
    private String source;
    private Map<String, Object> metadata;

    protected RawFlightReading() {
        // requis par Spring Data
    }

    public RawFlightReading(Long aircraftId, Instant recordedAt, int cycles, double maxLoadFactor,
                            double flightHours, Instant receivedAt, String source, Map<String, Object> metadata) {
        this.aircraftId = aircraftId;
        this.recordedAt = recordedAt;
        this.cycles = cycles;
        this.maxLoadFactor = maxLoadFactor;
        this.flightHours = flightHours;
        this.receivedAt = receivedAt;
        this.source = source;
        this.metadata = metadata;
    }

    public String getId() {
        return id;
    }

    public Long getAircraftId() {
        return aircraftId;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public int getCycles() {
        return cycles;
    }

    public double getMaxLoadFactor() {
        return maxLoadFactor;
    }

    public double getFlightHours() {
        return flightHours;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public String getSource() {
        return source;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
