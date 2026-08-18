package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.aircraft.AircraftService;
import dev.ynzi.fatiguetracker.fatigue.dto.FatigueStatusResponse;
import dev.ynzi.fatiguetracker.fatigue.dto.FleetFatigueResponse;
import dev.ynzi.fatiguetracker.fatigue.dto.FleetSummaryResponse;
import dev.ynzi.fatiguetracker.fatigue.dto.RecomputeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.batch.core.JobExecution;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Fatigue", description = "Recalcul (Spring Batch) et consultation de l'indice de fatigue")
public class FatigueController {

    private final FatigueService fatigueService;
    private final AircraftService aircraftService;

    public FatigueController(FatigueService fatigueService, AircraftService aircraftService) {
        this.fatigueService = fatigueService;
        this.aircraftService = aircraftService;
    }

    @PostMapping("/api/fatigue/recompute")
    @Operation(summary = "Recalculer la fatigue de la flotte",
            description = "Lance le job Spring Batch de recalcul pour tous les appareils. Réservé au rôle MAINT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recalcul lancé et résumé de l'exécution retourné"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Rôle MAINT requis"),
            @ApiResponse(responseCode = "500", description = "Impossible de lancer le recalcul")
    })
    public RecomputeResponse recompute() {
        JobExecution execution = fatigueService.recompute();
        return RecomputeResponse.from(execution);
    }

    @GetMapping("/api/aircraft/{id}/fatigue")
    @Operation(summary = "Consulter la fatigue d'un appareil",
            description = "Retourne son dernier statut calculé, ou un état non calculé si aucun recalcul n'a eu lieu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statut de fatigue retourné"),
            @ApiResponse(responseCode = "404", description = "Appareil introuvable")
    })
    public FatigueStatusResponse getForAircraft(@PathVariable Long id) {
        // findById lève AircraftNotFoundException (404) si l'appareil n'existe pas.
        aircraftService.findById(id);
        return fatigueService.findByAircraftId(id)
                .map(FatigueStatusResponse::from)
                .orElseGet(() -> FatigueStatusResponse.notComputed(id));
    }

    @GetMapping("/api/fatigue")
    @Operation(summary = "Consulter la fatigue de la flotte",
            description = "Retourne tous les derniers statuts calculés et isole les alertes de maintenance.")
    @ApiResponse(responseCode = "200", description = "État de fatigue de la flotte retourné")
    public FleetFatigueResponse getFleet() {
        return FleetFatigueResponse.from(fatigueService.findAll());
    }

    @GetMapping("/api/fleet/summary")
    @Operation(summary = "Consulter le résumé de la flotte",
            description = "Retourne le nombre d'appareils et les agrégats des derniers indices de fatigue calculés.")
    @ApiResponse(responseCode = "200", description = "Résumé agrégé de la flotte retourné")
    public FleetSummaryResponse getFleetSummary() {
        return FleetSummaryResponse.from(fatigueService.fleetSummary());
    }
}
