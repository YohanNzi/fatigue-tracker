package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.reading.dto.FlightReadingRequest;
import dev.ynzi.fatiguetracker.reading.dto.FlightReadingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/aircraft/{aircraftId}/readings")
@Tag(name = "Flight readings", description = "Ingestion et consultation des relevés de vol d'un appareil")
public class FlightReadingController {

    private final FlightReadingService flightReadingService;

    public FlightReadingController(FlightReadingService flightReadingService) {
        this.flightReadingService = flightReadingService;
    }

    @GetMapping
    @Operation(summary = "Lister les relevés de vol",
            description = "Retourne une page de relevés de l'appareil, triée par date croissante par défaut.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page de relevés retournée"),
            @ApiResponse(responseCode = "404", description = "Appareil introuvable")
    })
    public PagedModel<FlightReadingResponse> findByAircraft(
            @PathVariable Long aircraftId,
            @PageableDefault(size = 20, sort = "recordedAt", direction = Sort.Direction.ASC) Pageable pageable) {
        // Réponse paginée (enveloppe {content, page}) : la liste des relevés d'un
        // appareil est potentiellement grande, on ne la renvoie jamais en entier.
        // PagedModel plutôt qu'un Page<> brut → contrat de sérialisation stable et
        // documenté (évite l'avertissement Spring sur la sérialisation de PageImpl).
        return new PagedModel<>(
                flightReadingService.findByAircraft(aircraftId, pageable).map(FlightReadingResponse::from));
    }

    @PostMapping
    @Operation(summary = "Ajouter un relevé de vol",
            description = "Enregistre un relevé pour l'appareil et l'archive en best-effort. Réservé au rôle MAINT.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Relevé créé"),
            @ApiResponse(responseCode = "400", description = "Données du relevé invalides"),
            @ApiResponse(responseCode = "401", description = "Authentification requise"),
            @ApiResponse(responseCode = "403", description = "Rôle MAINT requis"),
            @ApiResponse(responseCode = "404", description = "Appareil introuvable")
    })
    public ResponseEntity<FlightReadingResponse> create(@PathVariable Long aircraftId,
                                                          @Valid @RequestBody FlightReadingRequest request,
                                                          UriComponentsBuilder uriComponentsBuilder) {
        FlightReading created = flightReadingService.create(aircraftId, request);
        // Pas de GET par id unitaire en J1 (seule la liste par appareil est exposée) :
        // la Location pointe donc vers la collection plutôt que vers une ressource
        // individuelle non résolvable.
        URI location = uriComponentsBuilder
                .path("/api/aircraft/{aircraftId}/readings")
                .buildAndExpand(aircraftId)
                .toUri();
        return ResponseEntity.created(location).body(FlightReadingResponse.from(created));
    }
}
