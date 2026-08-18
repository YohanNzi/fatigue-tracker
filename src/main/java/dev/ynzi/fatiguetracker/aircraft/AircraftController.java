package dev.ynzi.fatiguetracker.aircraft;

import dev.ynzi.fatiguetracker.aircraft.dto.AircraftRequest;
import dev.ynzi.fatiguetracker.aircraft.dto.AircraftResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/aircraft")
@Tag(name = "Aircraft", description = "CRUD des appareils de la flotte — lecture publique, écriture MAINT")
public class AircraftController {

    private final AircraftService aircraftService;

    public AircraftController(AircraftService aircraftService) {
        this.aircraftService = aircraftService;
    }

    @GetMapping
    @Operation(summary = "Lister les appareils", description = "Retourne tous les appareils enregistrés dans la flotte.")
    @ApiResponse(responseCode = "200", description = "Liste des appareils retournée")
    public List<AircraftResponse> findAll() {
        return aircraftService.findAll().stream()
                .map(AircraftResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un appareil", description = "Retourne le détail d'un appareil à partir de son identifiant.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appareil retourné"),
            @ApiResponse(responseCode = "404", description = "Appareil introuvable")
    })
    public AircraftResponse findById(@PathVariable Long id) {
        return AircraftResponse.from(aircraftService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un appareil", description = "Enregistre un nouvel appareil. Réservé au rôle MAINT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Appareil créé"),
            @ApiResponse(responseCode = "400", description = "Données de l'appareil invalides"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Rôle MAINT requis")
    })
    public ResponseEntity<AircraftResponse> create(@Valid @RequestBody AircraftRequest request,
                                                     UriComponentsBuilder uriComponentsBuilder) {
        Aircraft created = aircraftService.create(request);
        URI location = uriComponentsBuilder.path("/api/aircraft/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(AircraftResponse.from(created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un appareil", description = "Remplace les informations d'un appareil. Réservé au rôle MAINT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Appareil modifié"),
            @ApiResponse(responseCode = "400", description = "Données de l'appareil invalides"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Rôle MAINT requis"),
            @ApiResponse(responseCode = "404", description = "Appareil introuvable")
    })
    public AircraftResponse update(@PathVariable Long id, @Valid @RequestBody AircraftRequest request) {
        return AircraftResponse.from(aircraftService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un appareil", description = "Supprime un appareil de la flotte. Réservé au rôle MAINT.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Appareil supprimé"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Rôle MAINT requis"),
            @ApiResponse(responseCode = "404", description = "Appareil introuvable")
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aircraftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
