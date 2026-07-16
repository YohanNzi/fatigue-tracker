package dev.ynzi.fatiguetracker.fatigue.dto;

import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;

import java.util.List;

/** État de fatigue de toute la flotte, avec les appareils en alerte de maintenance isolés. */
public record FleetFatigueResponse(
        List<FatigueStatusResponse> aircraft,
        List<FatigueStatusResponse> maintenanceAlerts
) {

    public static FleetFatigueResponse from(List<FatigueStatus> statuses) {
        List<FatigueStatusResponse> all = statuses.stream()
                .map(FatigueStatusResponse::from)
                .toList();
        List<FatigueStatusResponse> alerts = all.stream()
                .filter(FatigueStatusResponse::maintenanceAlert)
                .toList();
        return new FleetFatigueResponse(all, alerts);
    }
}
