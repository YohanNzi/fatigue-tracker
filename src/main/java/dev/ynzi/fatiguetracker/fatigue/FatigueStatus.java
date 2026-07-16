package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

/**
 * Dernier indice de fatigue calculé pour un {@link Aircraft} (une ligne par appareil,
 * mise à jour en upsert par le job Spring Batch de recalcul — voir
 * {@code dev.ynzi.fatiguetracker.fatigue.batch}).
 * <p>
 * La formule de calcul est générique et illustrative (accumulation de dommage type
 * Miner), voir {@link FatigueCalculator} et le README : aucune donnée ni méthode
 * propriétaire d'un employeur réel n'est utilisée ici.
 */
@Entity
@Table(name = "fatigue_status")
public class FatigueStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aircraft_id", nullable = false, unique = true)
    private Aircraft aircraft;

    @Column(name = "fatigue_index", nullable = false)
    private double fatigueIndex;

    @Column(name = "readings_count", nullable = false)
    private int readingsCount;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    @Column(name = "maintenance_alert", nullable = false)
    private boolean maintenanceAlert;

    protected FatigueStatus() {
        // requis par JPA
    }

    public FatigueStatus(Aircraft aircraft, double fatigueIndex, int readingsCount, Instant computedAt,
                          boolean maintenanceAlert) {
        this.aircraft = aircraft;
        this.fatigueIndex = fatigueIndex;
        this.readingsCount = readingsCount;
        this.computedAt = computedAt;
        this.maintenanceAlert = maintenanceAlert;
    }

    public Long getId() {
        return id;
    }

    public Aircraft getAircraft() {
        return aircraft;
    }

    public double getFatigueIndex() {
        return fatigueIndex;
    }

    public void setFatigueIndex(double fatigueIndex) {
        this.fatigueIndex = fatigueIndex;
    }

    public int getReadingsCount() {
        return readingsCount;
    }

    public void setReadingsCount(int readingsCount) {
        this.readingsCount = readingsCount;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public void setComputedAt(Instant computedAt) {
        this.computedAt = computedAt;
    }

    public boolean isMaintenanceAlert() {
        return maintenanceAlert;
    }

    public void setMaintenanceAlert(boolean maintenanceAlert) {
        this.maintenanceAlert = maintenanceAlert;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FatigueStatus that)) {
            return false;
        }
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "FatigueStatus{id=%s, aircraftId=%s, fatigueIndex=%s, readingsCount=%s, computedAt=%s, maintenanceAlert=%s}"
                .formatted(id, aircraft != null ? aircraft.getId() : null, fatigueIndex, readingsCount, computedAt,
                        maintenanceAlert);
    }
}
