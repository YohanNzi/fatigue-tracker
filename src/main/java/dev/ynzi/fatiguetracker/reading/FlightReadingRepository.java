package dev.ynzi.fatiguetracker.reading;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightReadingRepository extends JpaRepository<FlightReading, Long> {

    List<FlightReading> findByAircraftIdOrderByRecordedAtAsc(Long aircraftId);
}
