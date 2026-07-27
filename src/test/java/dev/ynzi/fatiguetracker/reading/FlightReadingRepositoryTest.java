package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.AbstractIntegrationTest;
import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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
    void findByAircraftId_paged_returnsOnlyMatchingReadingsInSortedOrder() {
        Aircraft aircraft1 = aircraftRepository.save(new Aircraft("F-REP1", "Rafale", 100.0));
        Aircraft aircraft2 = aircraftRepository.save(new Aircraft("F-REP2", "Rafale", 200.0));

        Instant t1 = Instant.parse("2026-01-01T00:00:00Z");
        Instant t2 = Instant.parse("2026-02-01T00:00:00Z");

        flightReadingRepository.save(new FlightReading(aircraft1, t2, 5, 1.5, 2.0));
        flightReadingRepository.save(new FlightReading(aircraft1, t1, 2, 1.2, 1.0));
        flightReadingRepository.save(new FlightReading(aircraft2, t1, 9, 3.0, 4.0));

        Page<FlightReading> page = flightReadingRepository.findByAircraftId(
                aircraft1.getId(), PageRequest.of(0, 10, Sort.by("recordedAt").ascending()));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent().get(0).getRecordedAt()).isEqualTo(t1);
        assertThat(page.getContent().get(1).getRecordedAt()).isEqualTo(t2);
        assertThat(page.getContent()).allMatch(r -> r.getAircraft().getId().equals(aircraft1.getId()));
    }

    @Test
    void findByAircraftId_paged_limitsPageSizeAndReportsTotal() {
        Aircraft aircraft = aircraftRepository.save(new Aircraft("F-REP3", "Rafale", 300.0));
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        for (int i = 0; i < 3; i++) {
            flightReadingRepository.save(new FlightReading(aircraft, t.plusSeconds(i * 3600L), 1, 1.0, 1.0));
        }

        Page<FlightReading> firstPage = flightReadingRepository.findByAircraftId(
                aircraft.getId(), PageRequest.of(0, 2, Sort.by("recordedAt").ascending()));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void findAllWithAircraft_loadsEveryReadingWithItsAircraftInOneQuery() {
        Aircraft aircraft1 = aircraftRepository.save(new Aircraft("F-REP4", "Rafale", 100.0));
        Aircraft aircraft2 = aircraftRepository.save(new Aircraft("F-REP5", "Rafale", 200.0));
        Instant t = Instant.parse("2026-01-01T00:00:00Z");
        flightReadingRepository.save(new FlightReading(aircraft1, t, 2, 1.2, 1.0));
        flightReadingRepository.save(new FlightReading(aircraft2, t, 9, 3.0, 4.0));

        List<FlightReading> readings = flightReadingRepository.findAllWithAircraft();

        assertThat(readings).hasSize(2);
        // L'appareil est bien joint (accès hors session possible) : pas de LazyInitializationException.
        assertThat(readings).allMatch(r -> r.getAircraft().getRegistration() != null);
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
