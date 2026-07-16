package dev.ynzi.fatiguetracker.aircraft;

import dev.ynzi.fatiguetracker.aircraft.dto.AircraftRequest;
import dev.ynzi.fatiguetracker.aircraft.dto.AircraftResponse;
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
    public List<AircraftResponse> findAll() {
        return aircraftService.findAll().stream()
                .map(AircraftResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AircraftResponse findById(@PathVariable Long id) {
        return AircraftResponse.from(aircraftService.findById(id));
    }

    @PostMapping
    public ResponseEntity<AircraftResponse> create(@Valid @RequestBody AircraftRequest request,
                                                     UriComponentsBuilder uriComponentsBuilder) {
        Aircraft created = aircraftService.create(request);
        URI location = uriComponentsBuilder.path("/api/aircraft/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(AircraftResponse.from(created));
    }

    @PutMapping("/{id}")
    public AircraftResponse update(@PathVariable Long id, @Valid @RequestBody AircraftRequest request) {
        return AircraftResponse.from(aircraftService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        aircraftService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
