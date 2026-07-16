package dev.ynzi.fatiguetracker.aircraft;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

/**
 * Appareil suivi dans la flotte.
 * <p>
 * Seules les caractéristiques d'identification et le compteur global d'heures de
 * vol sont portées ici. L'indice de fatigue calculé à partir des relevés de vol
 * détaillés ({@link dev.ynzi.fatiguetracker.reading.FlightReading}) est porté par
 * une entité dédiée, {@link dev.ynzi.fatiguetracker.fatigue.FatigueStatus} (J2),
 * recalculée par un job Spring Batch (voir README).
 */
@Entity
@Table(name = "aircraft", uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = "registration"))
public class Aircraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String registration;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private double flightHours;

    protected Aircraft() {
        // requis par JPA
    }

    public Aircraft(String registration, String model, double flightHours) {
        this.registration = registration;
        this.model = model;
        this.flightHours = flightHours;
    }

    public Long getId() {
        return id;
    }

    public String getRegistration() {
        return registration;
    }

    public void setRegistration(String registration) {
        this.registration = registration;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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
        if (!(o instanceof Aircraft aircraft)) {
            return false;
        }
        // Tant que l'id n'est pas affecté (entité transiente), on compare sur
        // la clé métier (immatriculation) plutôt que de considérer deux
        // instances transientes distinctes comme toujours différentes.
        if (id != null && aircraft.id != null) {
            return id.equals(aircraft.id);
        }
        return Objects.equals(registration, aircraft.registration);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registration);
    }

    @Override
    public String toString() {
        return "Aircraft{id=%s, registration='%s', model='%s', flightHours=%s}"
                .formatted(id, registration, model, flightHours);
    }
}
