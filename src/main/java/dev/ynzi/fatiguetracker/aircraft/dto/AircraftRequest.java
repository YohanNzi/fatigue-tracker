package dev.ynzi.fatiguetracker.aircraft.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Corps de requête pour la création et la mise à jour d'un {@code Aircraft}.
 * Utilisé tel quel pour POST (création) et PUT (remplacement complet).
 */
public record AircraftRequest(

        @NotBlank(message = "L'immatriculation est obligatoire")
        String registration,

        @NotBlank(message = "Le modèle est obligatoire")
        String model,

        @PositiveOrZero(message = "Les heures de vol doivent être positives ou nulles")
        double flightHours
) {
}
