package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Vérifie les agrégats de flotte de façon isolée, sans charger de contexte Spring. */
@ExtendWith(MockitoExtension.class)
class FatigueServiceTest {

    @Mock
    private JobLauncher jobLauncher;

    @Mock
    private Job fatigueRecomputeJob;

    @Mock
    private FatigueStatusRepository fatigueStatusRepository;

    @Mock
    private AircraftRepository aircraftRepository;

    @InjectMocks
    private FatigueService fatigueService;

    @Test
    void fleetSummary_withComputedStatuses_returnsExpectedAggregates() {
        FatigueStatus first = status(10.0, false);
        FatigueStatus second = status(30.0, true);
        FatigueStatus third = status(50.0, true);
        when(fatigueStatusRepository.findAll()).thenReturn(List.of(first, second, third));
        when(aircraftRepository.count()).thenReturn(5L);

        FleetSummary summary = fatigueService.fleetSummary();

        assertThat(summary.totalAircraft()).isEqualTo(5);
        assertThat(summary.aircraftInAlert()).isEqualTo(2);
        assertThat(summary.averageFatigueIndex()).isEqualTo(30.0);
        assertThat(summary.maxFatigueIndex()).isEqualTo(50.0);
    }

    @Test
    void fleetSummary_withEmptyFleet_returnsZeros() {
        when(fatigueStatusRepository.findAll()).thenReturn(List.of());
        when(aircraftRepository.count()).thenReturn(0L);

        FleetSummary summary = fatigueService.fleetSummary();

        assertThat(summary.totalAircraft()).isZero();
        assertThat(summary.aircraftInAlert()).isZero();
        assertThat(summary.averageFatigueIndex()).isZero();
        assertThat(summary.maxFatigueIndex()).isZero();
    }

    private static FatigueStatus status(double fatigueIndex, boolean maintenanceAlert) {
        FatigueStatus status = mock(FatigueStatus.class);
        when(status.getFatigueIndex()).thenReturn(fatigueIndex);
        when(status.isMaintenanceAlert()).thenReturn(maintenanceAlert);
        return status;
    }
}
