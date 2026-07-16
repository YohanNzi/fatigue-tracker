package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Relevé de vol unitaire rattaché à un {@link Aircraft}.
 * <p>
 * Sert de matière première au calcul, en jalon ultérieur, d'un indice de fatigue
 * structurelle (voir README) — J1 se limite à la capture et à la persistance de ces relevés.
 */
@Entity
@Table(name = "flight_reading")
public class FlightReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id", nullable = false)
    private Aircraft aircraft;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(nullable = false)
    private int cycles;

    @Column(name = "max_load_factor", nullable = false)
    private double maxLoadFactor;

    @Column(name = "flight_hours", nullable = false)
    private double flightHours;

    protected FlightReading() {
        // requis par JPA
    }

    public FlightReading(Aircraft aircraft, Instant recordedAt, int cycles, double maxLoadFactor, double flightHours) {
        this.aircraft = aircraft;
        this.recordedAt = recordedAt;
        this.cycles = cycles;
        this.maxLoadFactor = maxLoadFactor;
        this.flightHours = flightHours;
    }

    public Long getId() {
        return id;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(Instant recordedAt) {
        this.recordedAt = recordedAt;
    }

    public int getCycles() {
        return cycles;
    }

    public void setCycles(int cycles) {
        this.cycles = cycles;
    }

    public double getMaxLoadFactor() {
        return maxLoadFactor;
    }

    public void setMaxLoadFactor(double maxLoadFactor) {
        this.maxLoadFactor = maxLoadFactor;
    }

    public double getFlightHours() {
        return flightHours;
    }

    public void setFlightHours(double flightHours) {
        this.flightHours = flightHours;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FlightReading that)) {
            return false;
        }
        // Entité sans clé métier naturelle : deux instances transientes ne sont
        // considérées égales que si elles sont le même objet.
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FlightReading{id=%s, aircraftId=%s, recordedAt=%s, cycles=%s, maxLoadFactor=%s, flightHours=%s}"
                .formatted(id, aircraft != null ? aircraft.getId() : null, recordedAt, cycles, maxLoadFactor, flightHours);
    }
}
