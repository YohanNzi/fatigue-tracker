package dev.ynzi.fatiguetracker.demo;

import dev.ynzi.fatiguetracker.demo.dto.DemoResetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demo")
@Tag(name = "Démonstration", description = "Réinitialisation des données de démonstration")
public class DemoController {

    private final DemoResetService demoResetService;

    public DemoController(DemoResetService demoResetService) {
        this.demoResetService = demoResetService;
    }

    @PostMapping("/reset")
    @Operation(summary = "Réinitialiser les données de démonstration",
            description = "Restaure la flotte et ses relevés depuis le seed de référence, puis recalcule la fatigue. "
                    + "Réservé au rôle MAINT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Données de démonstration restaurées"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Rôle MAINT requis")
    })
    public DemoResetResponse reset() {
        int aircraftSeeded = demoResetService.reset();
        return new DemoResetResponse(aircraftSeeded, "Données de démonstration restaurées");
    }
}
