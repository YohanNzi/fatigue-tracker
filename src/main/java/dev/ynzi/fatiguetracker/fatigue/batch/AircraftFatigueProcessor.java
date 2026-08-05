package dev.ynzi.fatiguetracker.fatigue.batch;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.fatigue.FatigueCalculator;
import dev.ynzi.fatiguetracker.fatigue.FatigueComputationResult;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;
import dev.ynzi.fatiguetracker.reading.FlightReading;
import dev.ynzi.fatiguetracker.reading.FlightReadingRepository;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Étape "processor" du job de recalcul : pour un appareil donné, retrouve ses relevés
 * de vol et délègue le calcul de l'indice de fatigue à {@link FatigueCalculator}
 * (formule pure, testée unitairement séparément).
 * <p>
 * <b>Anti N+1</b> : les relevés de toute la flotte sont chargés en <b>une seule
 * requête</b> au démarrage de l'étape ({@link #loadReadings()} sur {@code @BeforeStep})
 * puis regroupés par appareil en mémoire. Sans ça, {@code process(...)} émettait une
 * requête « relevés » par appareil traité (N+1). La carte est reconstruite à chaque
 * exécution du job, donc un rerun voit bien les relevés ajoutés entre-temps.
 * <p>
 * <b>Compromis assumé</b> : cette approche « driving query + jointure en mémoire »
 * charge tous les relevés le temps du job — acceptable pour un recalcul flotte
 * déclenché à la demande. Pour une flotte à très gros volume, l'étape suivante serait
 * une agrégation ensembliste en SQL ({@code SUM(...) GROUP BY aircraft_id}) ou un step
 * partitionné ; c'est un choix documenté, pas un oubli.
 */
@Component
public class AircraftFatigueProcessor implements ItemProcessor<Aircraft, FatigueStatus> {

    private final FlightReadingRepository flightReadingRepository;
    private final FatigueCalculator fatigueCalculator;

    private Map<Long, List<FlightReading>> readingsByAircraftId = Map.of();

    public AircraftFatigueProcessor(FlightReadingRepository flightReadingRepository, FatigueCalculator fatigueCalculator) {
        this.flightReadingRepository = flightReadingRepository;
        this.fatigueCalculator = fatigueCalculator;
    }

    @BeforeStep
    public void loadReadings() {
        readingsByAircraftId = flightReadingRepository.findAllWithAircraft().stream()
                .collect(Collectors.groupingBy(reading -> reading.getAircraft().getId()));
    }

    @Override
    public FatigueStatus process(Aircraft aircraft) {
        List<FlightReading> readings = readingsByAircraftId.getOrDefault(aircraft.getId(), List.of());
        FatigueComputationResult result = fatigueCalculator.compute(readings);
        return FatigueStatus.fromComputation(aircraft, result, Instant.now());
    }
}
