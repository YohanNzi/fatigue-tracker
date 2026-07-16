package dev.ynzi.fatiguetracker.reading;

import dev.ynzi.fatiguetracker.reading.dto.FlightReadingRequest;
import dev.ynzi.fatiguetracker.reading.dto.FlightReadingResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/aircraft/{aircraftId}/readings")
public class FlightReadingController {

    private final FlightReadingService flightReadingService;

    public FlightReadingController(FlightReadingService flightReadingService) {
        this.flightReadingService = flightReadingService;
    }

    @GetMapping
    public List<FlightReadingResponse> findByAircraft(@PathVariable Long aircraftId) {
        return flightReadingService.findByAircraft(aircraftId).stream()
                .map(FlightReadingResponse::from)
                .toList();
    }

    @PostMapping
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
