package dev.ynzi.fatiguetracker.fatigue.batch;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.fatigue.FatigueCalculator;
import dev.ynzi.fatiguetracker.fatigue.FatigueComputationResult;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;
import dev.ynzi.fatiguetracker.reading.FlightReading;
import dev.ynzi.fatiguetracker.reading.FlightReadingRepository;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Étape "processor" du job de recalcul : pour un appareil donné, charge ses relevés
 * de vol et délègue le calcul de l'indice de fatigue à {@link FatigueCalculator}
 * (formule pure, testée unitairement séparément).
 */
@Component
public class AircraftFatigueProcessor implements ItemProcessor<Aircraft, FatigueStatus> {

    private final FlightReadingRepository flightReadingRepository;
    private final FatigueCalculator fatigueCalculator;

    public AircraftFatigueProcessor(FlightReadingRepository flightReadingRepository, FatigueCalculator fatigueCalculator) {
        this.flightReadingRepository = flightReadingRepository;
        this.fatigueCalculator = fatigueCalculator;
    }

    @Override
    public FatigueStatus process(Aircraft aircraft) {
        List<FlightReading> readings = flightReadingRepository.findByAircraftIdOrderByRecordedAtAsc(aircraft.getId());
        FatigueComputationResult result = fatigueCalculator.compute(readings);
        return new FatigueStatus(aircraft, result.fatigueIndex(), result.readingsCount(), Instant.now(),
                result.maintenanceAlert());
    }
}
