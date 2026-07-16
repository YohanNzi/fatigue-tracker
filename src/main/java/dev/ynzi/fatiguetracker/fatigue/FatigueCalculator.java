package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.reading.FlightReading;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Calcul de l'indice de fatigue cumulée d'un appareil à partir de ses relevés de vol.
 * <p>
 * <b>Formule générique et illustrative</b>, inspirée (très librement) d'une accumulation
 * de dommage linéaire façon règle de Miner : chaque relevé contribue proportionnellement
 * à son nombre de cycles et à un facteur de charge maximal élevé à un exposant
 * (accentuant le poids des relevés au-delà d'un facteur de charge "nominal" de
 * référence, à la manière d'une courbe S-N simplifiée) :
 *
 * <pre>
 *   dommage_brut = Σ cycles_i × (maxLoadFactor_i / referenceLoadFactor) ^ exponent
 *   indice       = dommage_brut / normalizationFactor
 *   alerte       = indice ≥ alertThreshold
 * </pre>
 * <p>
 * Aucune donnée ni méthode propriétaire d'un employeur réel n'est utilisée : les
 * paramètres ({@code fatigue.*} dans {@code application.yml}) sont volontairement
 * arbitraires et configurables, à ajuster librement pour la démonstration.
 */
@Component
public class FatigueCalculator {

    private final FatigueProperties properties;

    public FatigueCalculator(FatigueProperties properties) {
        this.properties = properties;
    }

    public FatigueComputationResult compute(List<FlightReading> readings) {
        if (readings.isEmpty()) {
            return new FatigueComputationResult(0.0, 0, false);
        }

        double rawDamage = readings.stream()
                .mapToDouble(this::contributionOf)
                .sum();

        double fatigueIndex = rawDamage / properties.normalizationFactor();
        boolean maintenanceAlert = fatigueIndex >= properties.alertThreshold();

        return new FatigueComputationResult(fatigueIndex, readings.size(), maintenanceAlert);
    }

    private double contributionOf(FlightReading reading) {
        double loadRatio = reading.getMaxLoadFactor() / properties.referenceLoadFactor();
        return reading.getCycles() * Math.pow(loadRatio, properties.exponent());
    }
}
