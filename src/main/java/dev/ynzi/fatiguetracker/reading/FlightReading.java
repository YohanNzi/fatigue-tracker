package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.common.DomainRuleViolationException;
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

import static dev.ynzi.fatiguetracker.common.DomainRuleViolationException.requirePositiveOrZero;
import static dev.ynzi.fatiguetracker.common.DomainRuleViolationException.requirePresent;
import static dev.ynzi.fatiguetracker.common.DomainRuleViolationException.requireStrictlyPositive;
import java.util.Objects;

/**
 * Relevé de vol unitaire rattaché à un {@link Aircraft}.
 * <p>
 * Sert de matière première au calcul de l'indice de fatigue structurelle (formule
 * illustrative, voir {@link dev.ynzi.fatiguetracker.fatigue.FatigueCalculator} et le
 * README) — ce package se limite à la capture et à la persistance des relevés.
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

    /**
     * Un relevé est un fait mesuré : il est validé à la construction puis immuable (aucun
     * setter). Le facteur de charge doit être strictement positif — il est élevé à une
     * puissance dans {@link dev.ynzi.fatiguetracker.fatigue.FatigueCalculator} : une valeur
     * négative produirait une contribution négative et pourrait masquer une alerte de
     * maintenance.
     */
    public FlightReading(Aircraft aircraft, Instant recordedAt, int cycles, double maxLoadFactor, double flightHours) {
        this.aircraft = requirePresent(aircraft, "Un relevé de vol doit être rattaché à un appareil");
        this.recordedAt = requirePresent(recordedAt, "La date du relevé est obligatoire");
        if (cycles < 0) {
            throw new DomainRuleViolationException("Le nombre de cycles doit être positif ou nul");
        }
        this.cycles = cycles;
        this.maxLoadFactor = requireStrictlyPositive(maxLoadFactor, "Le facteur de charge maximal doit être strictement positif");
        this.flightHours = requirePositiveOrZero(flightHours, "Les heures de vol doivent être positives ou nulles");
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

    public int getCycles() {
        return cycles;
    }

    public double getMaxLoadFactor() {
        return maxLoadFactor;
    }

    public double getFlightHours() {
        return flightHours;
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
