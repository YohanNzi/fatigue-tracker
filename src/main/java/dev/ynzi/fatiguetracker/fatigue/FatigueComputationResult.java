package dev.ynzi.fatiguetracker.fatigue;

/** Résultat pur (sans effet de bord) du calcul d'indice de fatigue pour un appareil. */
public record FatigueComputationResult(double fatigueIndex, int readingsCount, boolean maintenanceAlert) {
}
