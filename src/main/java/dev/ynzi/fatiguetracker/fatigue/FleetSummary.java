package dev.ynzi.fatiguetracker.fatigue;

/** Valeurs agrégées des derniers statuts de fatigue calculés pour la flotte. */
public record FleetSummary(
        int totalAircraft,
        int aircraftInAlert,
        double averageFatigueIndex,
        double maxFatigueIndex
) {
}
