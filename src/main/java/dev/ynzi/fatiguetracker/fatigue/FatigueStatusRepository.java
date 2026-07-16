package dev.ynzi.fatiguetracker.fatigue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FatigueStatusRepository extends JpaRepository<FatigueStatus, Long> {

    Optional<FatigueStatus> findByAircraftId(Long aircraftId);

    List<FatigueStatus> findAllByOrderByAircraft_IdAsc();
}
