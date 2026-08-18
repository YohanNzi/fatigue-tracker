package dev.ynzi.fatiguetracker.fatigue.dto;

import dev.ynzi.fatiguetracker.fatigue.FleetSummary;
import io.swagger.v3.oas.annotations.media.Schema;

/** Résumé agrégé de la flotte et des derniers indices de fatigue calculés. */
public record FleetSummaryResponse(
        @Schema(description = "Nombre total d'appareils enregistrés", example = "5")
        int totalAircraft,
        @Schema(description = "Nombre d'appareils dont le dernier statut calculé est en alerte", example = "2")
        int aircraftInAlert,
        @Schema(description = "Moyenne des indices de fatigue calculés, ou 0 si aucun statut n'existe", example = "34.7")
        double averageFatigueIndex,
        @Schema(description = "Indice de fatigue calculé maximal, ou 0 si aucun statut n'existe", example = "92.4")
        double maxFatigueIndex
) {

    public static FleetSummaryResponse from(FleetSummary summary) {
        return new FleetSummaryResponse(
                summary.totalAircraft(),
                summary.aircraftInAlert(),
                summary.averageFatigueIndex(),
                summary.maxFatigueIndex()
        );
    }
}
