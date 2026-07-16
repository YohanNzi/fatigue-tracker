package dev.ynzi.fatiguetracker.fatigue.dto;

import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;

import java.time.Instant;

/**
 * Représentation exposée en sortie d'API — ne jamais renvoyer l'entité JPA directement.
 *
 * @param computed {@code false} tant qu'aucune exécution du job de recalcul n'a encore
 *                 produit de résultat pour cet appareil (voir {@link #notComputed}) ;
 *                 dans ce cas {@code fatigueIndex} vaut 0 et {@code computedAt} est nul.
 */
public record FatigueStatusResponse(
        Long aircraftId,
        double fatigueIndex,
        int readingsCount,
        Instant computedAt,
        boolean maintenanceAlert,
        boolean computed
) {

    public static FatigueStatusResponse from(FatigueStatus status) {
        return new FatigueStatusResponse(
                status.getAircraft().getId(),
                status.getFatigueIndex(),
                status.getReadingsCount(),
                status.getComputedAt(),
                status.isMaintenanceAlert(),
                true
        );
    }

    public static FatigueStatusResponse notComputed(Long aircraftId) {
        return new FatigueStatusResponse(aircraftId, 0.0, 0, null, false, false);
    }
}
