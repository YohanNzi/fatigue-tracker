package dev.ynzi.fatiguetracker.fatigue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FatigueStatusRepository extends JpaRepository<FatigueStatus, Long> {

    Optional<FatigueStatus> findByAircraftId(Long aircraftId);

    /** Statuts existants d'un lot d'appareils en une requête (upsert par chunk du job Batch). */
    List<FatigueStatus> findByAircraftIdIn(Collection<Long> aircraftIds);

    List<FatigueStatus> findAllByOrderByAircraft_IdAsc();
}
