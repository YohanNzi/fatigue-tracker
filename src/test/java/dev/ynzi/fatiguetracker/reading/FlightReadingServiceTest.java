package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftService;
import dev.ynzi.fatiguetracker.common.DomainRuleViolationException;
import dev.ynzi.fatiguetracker.reading.dto.FlightReadingRequest;
import dev.ynzi.fatiguetracker.reading.raw.RawFlightReadingRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Un relevé est une lecture de compteur : il fait foi pour les heures de l'appareil. */
class FlightReadingServiceTest {

    private final FlightReadingRepository readingRepository = mock(FlightReadingRepository.class);
    private final RawFlightReadingRepository rawRepository = mock(RawFlightReadingRepository.class);
    private final AircraftService aircraftService = mock(AircraftService.class);
    private final FlightReadingService service = new FlightReadingService(readingRepository, rawRepository, aircraftService);

    @Test
    void create_updatesAircraftCounterFromReading() {
        Aircraft aircraft = new Aircraft("F-GKXA", "Airbus A320-214", 41_250.0);
        when(aircraftService.findById(1L)).thenReturn(aircraft);
        when(readingRepository.save(any(FlightReading.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(1L, new FlightReadingRequest(Instant.parse("2026-09-01T08:00:00Z"), 1_500, 1.9, 41_900.0));

        assertThat(aircraft.getFlightHours()).isEqualTo(41_900.0);
    }

    @Test
    void create_withInvalidReading_leavesAircraftCounterUntouchedAndSavesNothing() {
        Aircraft aircraft = new Aircraft("F-GKXA", "Airbus A320-214", 41_250.0);
        when(aircraftService.findById(1L)).thenReturn(aircraft);

        assertThatThrownBy(() -> service.create(1L,
                new FlightReadingRequest(Instant.parse("2026-09-01T08:00:00Z"), 100, -10.0, 41_900.0)))
                .isInstanceOf(DomainRuleViolationException.class);

        assertThat(aircraft.getFlightHours()).isEqualTo(41_250.0);
        verify(readingRepository, never()).save(any());
    }
}
