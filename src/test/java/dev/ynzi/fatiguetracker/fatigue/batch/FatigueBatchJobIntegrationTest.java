package dev.ynzi.fatiguetracker.fatigue.batch;

import dev.ynzi.fatiguetracker.AbstractIntegrationTest;
import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatusRepository;
import dev.ynzi.fatiguetracker.reading.FlightReading;
import dev.ynzi.fatiguetracker.reading.FlightReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test d'intégration du job Spring Batch {@code fatigueRecomputeJob} contre un vrai
 * Postgres (Testcontainers, migrations Flyway V2/V3 comprises) : insère des appareils
 * et relevés, lance réellement le job via {@link JobLauncherTestUtils}, vérifie
 * l'indice et l'alerte persistés. Skippé proprement sans Docker (voir
 * {@link AbstractIntegrationTest}).
 * <p>
 * Paramètres de la formule utilisés : ceux d'application.yml (référence 1.0,
 * exposant 3.0, normalisation 1000.0, seuil d'alerte 80.0) — les valeurs de test
 * sont choisies pour rester très en-dessous ou très au-dessus du seuil, sans
 * dépendre d'un calcul au bord.
 */
@SpringBootTest
@SpringBatchTest
class FatigueBatchJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private FlightReadingRepository flightReadingRepository;

    @Autowired
    private FatigueStatusRepository fatigueStatusRepository;

    private Long lowFatigueAircraftId;
    private Long highFatigueAircraftId;
    private Long noReadingsAircraftId;

    @BeforeEach
    void setUp() {
        // État propre à chaque test : le job traite tous les appareils via findAll().
        flightReadingRepository.deleteAll();
        fatigueStatusRepository.deleteAll();
        aircraftRepository.deleteAll();

        Aircraft lowFatigueAircraft = aircraftRepository.save(new Aircraft("F-BATCH1", "Rafale", 100.0));
        Aircraft highFatigueAircraft = aircraftRepository.save(new Aircraft("F-BATCH2", "Rafale", 200.0));
        Aircraft noReadingsAircraft = aircraftRepository.save(new Aircraft("F-BATCH3", "Rafale", 0.0));

        lowFatigueAircraftId = lowFatigueAircraft.getId();
        highFatigueAircraftId = highFatigueAircraft.getId();
        noReadingsAircraftId = noReadingsAircraft.getId();

        Instant t = Instant.parse("2026-01-01T00:00:00Z");

        // Faible sollicitation : index très inférieur au seuil par défaut (80.0).
        flightReadingRepository.save(new FlightReading(lowFatigueAircraft, t, 2, 1.0, 1.0));

        // Forte sollicitation (cycles et facteur de charge élevés) : franchit largement le seuil.
        flightReadingRepository.save(new FlightReading(highFatigueAircraft, t, 1000, 5.0, 5.0));
        flightReadingRepository.save(new FlightReading(highFatigueAircraft, t.plusSeconds(3600), 1000, 5.0, 5.0));

        // noReadingsAircraft : aucun relevé.
    }

    @Test
    void recomputeJob_persistsFatigueStatusForEveryAircraft() throws Exception {
        JobExecution execution = jobLauncherTestUtils.launchJob();

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        Optional<FatigueStatus> lowStatus = fatigueStatusRepository.findByAircraftId(lowFatigueAircraftId);
        assertThat(lowStatus).isPresent();
        assertThat(lowStatus.get().getReadingsCount()).isEqualTo(1);
        assertThat(lowStatus.get().isMaintenanceAlert()).isFalse();

        Optional<FatigueStatus> highStatus = fatigueStatusRepository.findByAircraftId(highFatigueAircraftId);
        assertThat(highStatus).isPresent();
        assertThat(highStatus.get().getReadingsCount()).isEqualTo(2);
        assertThat(highStatus.get().isMaintenanceAlert()).isTrue();
        assertThat(highStatus.get().getFatigueIndex()).isGreaterThan(80.0);

        Optional<FatigueStatus> noReadingsStatus = fatigueStatusRepository.findByAircraftId(noReadingsAircraftId);
        assertThat(noReadingsStatus).isPresent();
        assertThat(noReadingsStatus.get().getReadingsCount()).isZero();
        assertThat(noReadingsStatus.get().getFatigueIndex()).isZero();
        assertThat(noReadingsStatus.get().isMaintenanceAlert()).isFalse();
    }

    @Test
    void recomputeJob_rerun_upsertsRatherThanDuplicating() throws Exception {
        jobLauncherTestUtils.launchJob();

        flightReadingRepository.save(new FlightReading(
                aircraftRepository.findById(lowFatigueAircraftId).orElseThrow(),
                Instant.parse("2026-02-01T00:00:00Z"), 3, 1.2, 1.0));

        jobLauncherTestUtils.launchJob();

        assertThat(fatigueStatusRepository.findAll())
                .filteredOn(status -> status.getAircraft().getId().equals(lowFatigueAircraftId))
                .hasSize(1);
        assertThat(fatigueStatusRepository.findByAircraftId(lowFatigueAircraftId).orElseThrow().getReadingsCount())
                .isEqualTo(2);
    }
}
