package dev.ynzi.fatiguetracker.fatigue;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de la formule illustrative de calcul d'indice de fatigue et du job
 * Spring Batch associé (voir {@link FatigueCalculator} et application.yml).
 *
 * @param referenceLoadFactor facteur de charge de référence ("nominal"), &gt; 0
 * @param exponent            exposant appliqué au ratio de charge (accentue les relevés
 *                            au-delà du nominal, façon courbe S-N)
 * @param normalizationFactor diviseur appliqué au dommage brut cumulé pour obtenir un
 *                            indice dans un ordre de grandeur lisible
 * @param alertThreshold      seuil au-delà duquel {@code maintenanceAlert} est levé
 * @param chunkSize           taille de chunk du step Spring Batch de recalcul
 */
@ConfigurationProperties(prefix = "fatigue")
public record FatigueProperties(
        double referenceLoadFactor,
        double exponent,
        double normalizationFactor,
        double alertThreshold,
        int chunkSize
) {

    public FatigueProperties {
        if (referenceLoadFactor <= 0) {
            throw new IllegalArgumentException("fatigue.reference-load-factor doit être strictement positif");
        }
        if (normalizationFactor <= 0) {
            throw new IllegalArgumentException("fatigue.normalization-factor doit être strictement positif");
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("fatigue.chunk-size doit être strictement positif");
        }
    }
}
