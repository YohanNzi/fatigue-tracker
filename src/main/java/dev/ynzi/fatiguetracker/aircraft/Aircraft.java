package dev.ynzi.fatiguetracker.aircraft;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

import static dev.ynzi.fatiguetracker.common.DomainRuleViolationException.requirePositiveOrZero;
import static dev.ynzi.fatiguetracker.common.DomainRuleViolationException.requireText;

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
        replaceDetails(registration, model, flightHours);
    }

    /**
     * Remplace l'ensemble des caractéristiques de l'appareil (sémantique du PUT : correction
     * complète, y compris une remise à niveau manuelle du compteur d'heures). Mêmes règles
     * qu'à la création : l'appareil ne peut pas passer par un état partiel ou invalide.
     */
    public void replaceDetails(String registration, String model, double flightHours) {
        // Tout valider avant d'affecter : un remplacement refusé ne laisse pas d'état partiel.
        String validRegistration = requireText(registration, "L'immatriculation est obligatoire");
        String validModel = requireText(model, "Le modèle est obligatoire");
        double validFlightHours = requirePositiveOrZero(flightHours, "Les heures de vol doivent être positives ou nulles");
        this.registration = validRegistration;
        this.model = validModel;
        this.flightHours = validFlightHours;
    }

    /**
     * Prend en compte la lecture de compteur d'un relevé : le compteur d'heures cumulées de
     * l'appareil suit le relevé le plus récent. Un compteur ne recule pas — un relevé
     * rétroactif (plus ancien, donc de valeur inférieure) est conservé comme historique mais
     * ne fait pas baisser le compteur. Une seule source de vérité : l'appareil porte la
     * dernière valeur connue, les relevés en sont l'historique.
     */
    public void recordCounterReading(double counterFlightHours) {
        double reading = requirePositiveOrZero(counterFlightHours, "Le compteur d'heures doit être positif ou nul");
        this.flightHours = Math.max(this.flightHours, reading);
    }

    public Long getId() {
        return id;
    }

    public String getRegistration() {
        return registration;
    }

    public String getModel() {
        return model;
    }

    public double getFlightHours() {
        return flightHours;
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
