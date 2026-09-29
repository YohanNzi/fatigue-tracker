package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.common.DomainRuleViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invariants du relevé de vol : un relevé physiquement impossible ne peut pas exister. */
class FlightReadingTest {

    private static final Aircraft AIRCRAFT = new Aircraft("F-TEST", "A320", 100.0);
    private static final Instant RECORDED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void validReading_isCreated() {
        FlightReading reading = new FlightReading(AIRCRAFT, RECORDED_AT, 0, 1.2, 0.0);

        assertThat(reading.getCycles()).isZero();
        assertThat(reading.getMaxLoadFactor()).isEqualTo(1.2);
    }

    /** Le facteur de charge est élevé au cube dans le calcul : ≤ 0 pourrait masquer une alerte. */
    @ParameterizedTest
    @ValueSource(doubles = {0.0, -10.0, Double.NaN, Double.POSITIVE_INFINITY})
    void nonPositiveOrNonFiniteLoadFactor_isRejected(double maxLoadFactor) {
        assertThatThrownBy(() -> new FlightReading(AIRCRAFT, RECORDED_AT, 100, maxLoadFactor, 1.0))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("facteur de charge");
    }

    @Test
    void negativeCycles_areRejected() {
        assertThatThrownBy(() -> new FlightReading(AIRCRAFT, RECORDED_AT, -1, 1.0, 1.0))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("cycles");
    }

    @Test
    void negativeFlightHours_areRejected() {
        assertThatThrownBy(() -> new FlightReading(AIRCRAFT, RECORDED_AT, 1, 1.0, -0.5))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("heures de vol");
    }

    @Test
    void readingWithoutAircraft_isRejected() {
        assertThatThrownBy(() -> new FlightReading(null, RECORDED_AT, 1, 1.0, 1.0))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("appareil");
    }

    @Test
    void readingWithoutDate_isRejected() {
        assertThatThrownBy(() -> new FlightReading(AIRCRAFT, null, 1, 1.0, 1.0))
                .isInstanceOf(DomainRuleViolationException.class)
                .hasMessageContaining("date");
    }
}
