package dev.ynzi.fatiguetracker.aircraft.dto;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;

/** Représentation exposée en sortie d'API — ne jamais renvoyer l'entité JPA directement. */
public record AircraftResponse(
        Long id,
        String registration,
        String model,
        double flightHours
) {

    public static AircraftResponse from(Aircraft aircraft) {
        return new AircraftResponse(
                aircraft.getId(),
                aircraft.getRegistration(),
                aircraft.getModel(),
                aircraft.getFlightHours()
        );
    }
}
