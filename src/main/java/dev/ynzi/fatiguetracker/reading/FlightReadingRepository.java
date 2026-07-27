package dev.ynzi.fatiguetracker.reading;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FlightReadingRepository extends JpaRepository<FlightReading, Long> {

    /**
     * Relevés d'un appareil, paginés. La pagination borne la réponse de l'API
     * ({@code GET /api/aircraft/{id}/readings}) : un appareil peut accumuler un grand
     * nombre de relevés, on ne renvoie donc jamais la collection entière d'un coup.
     * L'ordre est porté par le {@link Pageable} (tri par défaut {@code recordedAt} ASC,
     * voir {@link FlightReadingController}).
     */
    Page<FlightReading> findByAircraftId(Long aircraftId, Pageable pageable);

    /**
     * Charge tous les relevés de la flotte en <b>une seule requête</b>, avec leur
     * appareil (jointure), pour le recalcul de fatigue par lot. Évite le N+1 du job
     * (sinon une requête « relevés » par appareil traité) : le processor regroupe
     * ensuite ces relevés par appareil en mémoire.
     */
    @Query("select r from FlightReading r join fetch r.aircraft")
    List<FlightReading> findAllWithAircraft();
}
