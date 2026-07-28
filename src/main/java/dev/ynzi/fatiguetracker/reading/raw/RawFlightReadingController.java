package dev.ynzi.fatiguetracker.reading.raw;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consultation des relevés bruts archivés dans MongoDB (J5.5), paginés, du plus récent au
 * plus ancien. Lecture publique (comme le reste de l'API en lecture) ; l'archivage lui-même
 * se fait à l'ingestion d'un relevé (voir {@link dev.ynzi.fatiguetracker.reading.FlightReadingService}).
 */
@RestController
@RequestMapping("/api/aircraft/{aircraftId}/raw-readings")
@Tag(name = "Raw readings", description = "Relevés bruts archivés (MongoDB, persistance polyglotte)")
public class RawFlightReadingController {

    private final RawFlightReadingRepository rawFlightReadingRepository;

    public RawFlightReadingController(RawFlightReadingRepository rawFlightReadingRepository) {
        this.rawFlightReadingRepository = rawFlightReadingRepository;
    }

    @GetMapping
    public PagedModel<RawFlightReadingResponse> list(
            @PathVariable Long aircraftId,
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(
                rawFlightReadingRepository.findByAircraftId(aircraftId, pageable).map(RawFlightReadingResponse::from));
    }
}
