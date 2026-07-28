package dev.ynzi.fatiguetracker.reading.raw;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RawFlightReadingRepository extends MongoRepository<RawFlightReading, String> {

    Page<RawFlightReading> findByAircraftId(Long aircraftId, Pageable pageable);
}
