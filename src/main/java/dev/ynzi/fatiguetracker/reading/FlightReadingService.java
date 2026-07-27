package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftService;
import dev.ynzi.fatiguetracker.reading.dto.FlightReadingRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FlightReadingService {

    private final FlightReadingRepository flightReadingRepository;
    private final AircraftService aircraftService;

    public FlightReadingService(FlightReadingRepository flightReadingRepository, AircraftService aircraftService) {
        this.flightReadingRepository = flightReadingRepository;
        this.aircraftService = aircraftService;
    }

    public Page<FlightReading> findByAircraft(Long aircraftId, Pageable pageable) {
        // findById lève AircraftNotFoundException (404) si l'appareil n'existe pas.
        aircraftService.findById(aircraftId);
        return flightReadingRepository.findByAircraftId(aircraftId, pageable);
    }

    @Transactional
    public FlightReading create(Long aircraftId, FlightReadingRequest request) {
        Aircraft aircraft = aircraftService.findById(aircraftId);
        FlightReading reading = new FlightReading(
                aircraft,
                request.recordedAt(),
                request.cycles(),
                request.maxLoadFactor(),
                request.flightHours()
        );
        return flightReadingRepository.save(reading);
    }
}
