package dev.ynzi.fatiguetracker.fatigue.batch;

import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatusRepository;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Étape "writer" du job de recalcul : upsert du {@link FatigueStatus} par appareil
 * (une ligne unique par appareil, mise à jour en place plutôt qu'un historique).
 */
@Component
public class FatigueStatusWriter implements ItemWriter<FatigueStatus> {

    private final FatigueStatusRepository fatigueStatusRepository;

    public FatigueStatusWriter(FatigueStatusRepository fatigueStatusRepository) {
        this.fatigueStatusRepository = fatigueStatusRepository;
    }

    @Override
    public void write(Chunk<? extends FatigueStatus> chunk) {
        for (FatigueStatus computed : chunk) {
            FatigueStatus toPersist = fatigueStatusRepository.findByAircraftId(computed.getAircraft().getId())
                    .map(existing -> applyComputed(existing, computed))
                    .orElse(computed);
            fatigueStatusRepository.save(toPersist);
        }
    }

    private FatigueStatus applyComputed(FatigueStatus existing, FatigueStatus computed) {
        existing.setFatigueIndex(computed.getFatigueIndex());
        existing.setReadingsCount(computed.getReadingsCount());
        existing.setComputedAt(computed.getComputedAt());
        existing.setMaintenanceAlert(computed.isMaintenanceAlert());
        return existing;
    }
}
