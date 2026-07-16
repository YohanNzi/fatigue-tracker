package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.aircraft.AircraftService;
import dev.ynzi.fatiguetracker.fatigue.dto.FatigueStatusResponse;
import dev.ynzi.fatiguetracker.fatigue.dto.FleetFatigueResponse;
import dev.ynzi.fatiguetracker.fatigue.dto.RecomputeResponse;
import org.springframework.batch.core.JobExecution;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FatigueController {

    private final FatigueService fatigueService;
    private final AircraftService aircraftService;

    public FatigueController(FatigueService fatigueService, AircraftService aircraftService) {
        this.fatigueService = fatigueService;
        this.aircraftService = aircraftService;
    }

    @PostMapping("/api/fatigue/recompute")
    public RecomputeResponse recompute() {
        JobExecution execution = fatigueService.recompute();
        return RecomputeResponse.from(execution);
    }

    @GetMapping("/api/aircraft/{id}/fatigue")
    public FatigueStatusResponse getForAircraft(@PathVariable Long id) {
        // findById lève AircraftNotFoundException (404) si l'appareil n'existe pas.
        aircraftService.findById(id);
        return fatigueService.findByAircraftId(id)
                .map(FatigueStatusResponse::from)
                .orElseGet(() -> FatigueStatusResponse.notComputed(id));
    }

    @GetMapping("/api/fatigue")
    public FleetFatigueResponse getFleet() {
        return FleetFatigueResponse.from(fatigueService.findAll());
    }
}
