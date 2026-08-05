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

    /**
     * Crée le statut de fatigue d'un appareil à partir d'un résultat de calcul (insertion).
     * Point d'entrée unique de construction depuis un {@link FatigueComputationResult}.
     */
    public static FatigueStatus fromComputation(Aircraft aircraft, FatigueComputationResult result, Instant computedAt) {
        return new FatigueStatus(aircraft, result.fatigueIndex(), result.readingsCount(), computedAt,
                result.maintenanceAlert());
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

    public int getReadingsCount() {
        return readingsCount;
    }

    public Instant getComputedAt() {
        return computedAt;
    }

    public boolean isMaintenanceAlert() {
        return maintenanceAlert;
    }

    /**
     * Réapplique en place les valeurs d'un recalcul fraîchement produit (branche
     * <em>update</em> de l'upsert du job Batch). L'entité garde la maîtrise de sa
     * mutation : plus de setters exposés champ par champ. L'identité ({@code id},
     * {@code aircraft}) n'est jamais modifiée ici.
     */
    public void applyComputedValuesFrom(FatigueStatus computed) {
        this.fatigueIndex = computed.fatigueIndex;
        this.readingsCount = computed.readingsCount;
        this.computedAt = computed.computedAt;
        this.maintenanceAlert = computed.maintenanceAlert;
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
