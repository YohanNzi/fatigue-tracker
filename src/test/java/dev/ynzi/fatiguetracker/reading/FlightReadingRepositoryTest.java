package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.AbstractIntegrationTest;
import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vérifie le repository {@link FlightReadingRepository} contre un vrai Postgres
 * (Testcontainers), migrations Flyway appliquées — plutôt qu'une base embarquée,
 * pour couvrir le schéma réellement exécuté en production. Skippé sans Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlightReadingRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private FlightReadingRepository flightReadingRepository;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Test
    void findByAircraftIdOrderByRecordedAtAsc_returnsOnlyMatchingReadingsInOrder() {
        Aircraft aircraft1 = aircraftRepository.save(new Aircraft("F-REP1", "Rafale", 100.0));
        Aircraft aircraft2 = aircraftRepository.save(new Aircraft("F-REP2", "Rafale", 200.0));

        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-02-01T00:00:00Z");

        flightReadingRepository.save(new FlightReading(aircraft1, t2, 5, 1.5, 2.0));
        flightReadingRepository.save(new FlightReading(aircraft1, t1, 2, 1.2, 1.0));
        flightReadingRepository.save(new FlightReading(aircraft2, t1, 9, 3.0, 4.0));

        List<FlightReading> readings = flightReadingRepository.findByAircraftIdOrderByRecordedAtAsc(aircraft1.getId());

        assertThat(readings).hasSize(2);
        assertThat(readings.get(0).getRecordedAt()).isEqualTo(t1);
        assertThat(readings.get(1).getRecordedAt()).isEqualTo(t2);
        assertThat(readings).allMatch(r -> r.getAircraft().getId().equals(aircraft1.getId()));
    }

    @Test
    void save_withoutAircraft_violatesNotNullConstraint() {
        FlightReading orphanReading = new FlightReading(null, Instant.now(), 1, 1.0, 1.0);

        org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    flightReadingRepository.saveAndFlush(orphanReading);
                }
        );
    }
}
