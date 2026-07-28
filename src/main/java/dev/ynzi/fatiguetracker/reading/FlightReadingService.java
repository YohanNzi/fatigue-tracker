package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftService;
import dev.ynzi.fatiguetracker.reading.dto.FlightReadingRequest;
import dev.ynzi.fatiguetracker.reading.raw.RawFlightReading;
import dev.ynzi.fatiguetracker.reading.raw.RawFlightReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional(readOnly = true)
public class FlightReadingService {

    private static final Logger log = LoggerFactory.getLogger(FlightReadingService.class);

    private final FlightReadingRepository flightReadingRepository;
    private final RawFlightReadingRepository rawFlightReadingRepository;
    private final AircraftService aircraftService;

    public FlightReadingService(FlightReadingRepository flightReadingRepository,
                                RawFlightReadingRepository rawFlightReadingRepository,
                                AircraftService aircraftService) {
        this.flightReadingRepository = flightReadingRepository;
        this.rawFlightReadingRepository = rawFlightReadingRepository;
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
        FlightReading saved = flightReadingRepository.save(reading);
        storeRaw(aircraftId, request);
        return saved;
    }

    /**
     * Archive le relevé brut dans MongoDB (zone d'atterrissage append-only). <b>Best-effort</b> :
     * un incident Mongo est journalisé mais ne fait pas échouer l'ingestion — la source de
     * vérité (PostgreSQL) reste committée, et le calcul de fatigue n'en dépend pas.
     */
    private void storeRaw(Long aircraftId, FlightReadingRequest request) {
        try {
            rawFlightReadingRepository.save(new RawFlightReading(
                    aircraftId,
                    request.recordedAt(),
                    request.cycles(),
                    request.maxLoadFactor(),
                    request.flightHours(),
                    Instant.now(),
                    "api",
                    request.metadata()
            ));
        } catch (RuntimeException e) {
            // Best-effort : couvre notamment les DataAccessException (Mongo indisponible).
            log.warn("Archivage du relevé brut (Mongo) échoué pour l'appareil {} : {}", aircraftId, e.getMessage());
        }
    }
}
