package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.reading.FlightReading;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie la formule illustrative de {@link FatigueCalculator} de façon isolée
 * (aucun contexte Spring nécessaire) : cas limites (0 relevé) et franchissement
 * ou non du seuil d'alerte, avec des paramètres fixés explicitement pour un
 * résultat entièrement déterministe.
 */
class FatigueCalculatorTest {

    private static final Aircraft AIRCRAFT = new Aircraft("F-TEST", "Rafale", 100.0);

    @Test
    void compute_withNoReadings_returnsZeroIndexAndNoAlert() {
        FatigueCalculator calculator = calculatorWith(1.0, 2.0, 10.0, 5.0);

        FatigueComputationResult result = calculator.compute(List.of());

        assertThat(result.fatigueIndex()).isZero();
        assertThat(result.readingsCount()).isZero();
        assertThat(result.maintenanceAlert()).isFalse();
    }

    @Test
    void compute_belowThreshold_doesNotRaiseAlert() {
        // reference=1.0, exponent=2.0 -> contribution = cycles * maxLoadFactor^2
        // 1 relevé : 5 * 2.0^2 = 20 ; index = 20 / normalizationFactor(10) = 2.0 < seuil 5.0
        FatigueCalculator calculator = calculatorWith(1.0, 2.0, 10.0, 5.0);
        FlightReading reading = reading(5, 2.0);

        FatigueComputationResult result = calculator.compute(List.of(reading));

        assertThat(result.fatigueIndex()).isEqualTo(2.0);
        assertThat(result.readingsCount()).isEqualTo(1);
        assertThat(result.maintenanceAlert()).isFalse();
    }

    @Test
    void compute_atOrAboveThreshold_raisesAlert() {
        // 2 relevés : (5*2.0^2=20) + (10*3.0^2=90) = 110 ; index = 110/10 = 11.0 >= seuil 5.0
        FatigueCalculator calculator = calculatorWith(1.0, 2.0, 10.0, 5.0);
        List<FlightReading> readings = List.of(reading(5, 2.0), reading(10, 3.0));

        FatigueComputationResult result = calculator.compute(readings);

        assertThat(result.fatigueIndex()).isEqualTo(11.0);
        assertThat(result.readingsCount()).isEqualTo(2);
        assertThat(result.maintenanceAlert()).isTrue();
    }

    @Test
    void compute_exactlyAtThreshold_raisesAlert() {
        // 1 relevé : 5 * 1.0^2 = 5 ; index = 5/10 = 0.5 ; seuil abaissé à 0.5 -> égalité incluse
        FatigueCalculator calculator = calculatorWith(1.0, 2.0, 10.0, 0.5);
        FlightReading reading = reading(5, 1.0);

        FatigueComputationResult result = calculator.compute(List.of(reading));

        assertThat(result.fatigueIndex()).isEqualTo(0.5);
        assertThat(result.maintenanceAlert()).isTrue();
    }

    private static FlightReading reading(int cycles, double maxLoadFactor) {
        return new FlightReading(AIRCRAFT, Instant.parse("2026-01-01T00:00:00Z"), cycles, maxLoadFactor, 1.0);
    }

    private static FatigueCalculator calculatorWith(double referenceLoadFactor, double exponent,
                                                      double normalizationFactor, double alertThreshold) {
        FatigueProperties properties = new FatigueProperties(referenceLoadFactor, exponent, normalizationFactor,
                alertThreshold, 20);
        return new FatigueCalculator(properties);
    }
}
