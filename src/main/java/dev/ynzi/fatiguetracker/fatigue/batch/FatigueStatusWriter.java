package dev.ynzi.fatiguetracker.fatigue.batch;

import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatusRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Étape "writer" du job de recalcul : upsert du {@link FatigueStatus} par appareil
 * (une ligne unique par appareil, mise à jour en place plutôt qu'un historique).
 * <p>
 * <b>Anti N+1</b> (même logique que {@link AircraftFatigueProcessor}) : les statuts déjà
 * existants du chunk sont chargés en <b>une seule requête</b>, puis l'upsert se fait en
 * mémoire — au lieu d'un {@code findByAircraftId} par appareil.
 */
@Component
public class FatigueStatusWriter implements ItemWriter<FatigueStatus> {

    private final FatigueStatusRepository fatigueStatusRepository;

    public FatigueStatusWriter(FatigueStatusRepository fatigueStatusRepository) {
        this.fatigueStatusRepository = fatigueStatusRepository;
    }

    @Override
    public void write(Chunk<? extends FatigueStatus> chunk) {
        List<Long> aircraftIds = chunk.getItems().stream()
                .map(computed -> computed.getAircraft().getId())
                .toList();
        Map<Long, FatigueStatus> existingByAircraftId = fatigueStatusRepository.findByAircraftIdIn(aircraftIds).stream()
                .collect(Collectors.toMap(status -> status.getAircraft().getId(), Function.identity()));

        List<FatigueStatus> toPersist = chunk.getItems().stream()
                .map(computed -> upsert(existingByAircraftId.get(computed.getAircraft().getId()), computed))
                .toList();
        fatigueStatusRepository.saveAll(toPersist);
    }

    private FatigueStatus upsert(FatigueStatus existing, FatigueStatus computed) {
        if (existing == null) {
            return computed;
        }
        existing.applyComputedValuesFrom(computed);
        return existing;
    }
}
