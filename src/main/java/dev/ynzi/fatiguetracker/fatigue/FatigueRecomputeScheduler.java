package dev.ynzi.fatiguetracker.fatigue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "fatigue.schedule", name = "enabled", havingValue = "true")
public class FatigueRecomputeScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(FatigueRecomputeScheduler.class);

    private final FatigueService fatigueService;

    public FatigueRecomputeScheduler(FatigueService fatigueService) {
        this.fatigueService = fatigueService;
    }

    @Scheduled(cron = "${fatigue.schedule.cron}")
    public void recomputeFatigue() {
        LOGGER.info("Déclenchement planifié du recalcul de fatigue");
        try {
            var jobExecution = fatigueService.recompute();
            LOGGER.info("Recalcul planifié de fatigue terminé avec le statut {}", jobExecution.getStatus());
        } catch (Exception e) {
            // Une exécution en échec ne doit pas interrompre les déclenchements suivants.
            LOGGER.error("Échec du recalcul planifié de fatigue", e);
        }
    }
}
