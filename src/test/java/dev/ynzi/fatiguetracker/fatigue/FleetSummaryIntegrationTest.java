package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.AbstractIntegrationTest;
import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import dev.ynzi.fatiguetracker.reading.FlightReading;
import dev.ynzi.fatiguetracker.reading.FlightReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test bout en bout du recalcul puis de l'agrégation du résumé de flotte. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FleetSummaryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private FlightReadingRepository flightReadingRepository;

    @Autowired
    private FatigueStatusRepository fatigueStatusRepository;

    @BeforeEach
    void setUp() {
        fatigueStatusRepository.deleteAll();
        flightReadingRepository.deleteAll();
        aircraftRepository.deleteAll();

        Aircraft low = aircraftRepository.save(new Aircraft("F-SUM01", "Rafale", 100.0));
        Aircraft high = aircraftRepository.save(new Aircraft("F-SUM02", "Rafale", 200.0));
        aircraftRepository.save(new Aircraft("F-SUM03", "Rafale", 50.0));

        Instant recordedAt = Instant.parse("2026-08-19T10:00:00Z");
        flightReadingRepository.save(new FlightReading(low, recordedAt, 2, 1.0, 1.0));
        flightReadingRepository.save(new FlightReading(high, recordedAt, 1000, 5.0, 5.0));
        flightReadingRepository.save(new FlightReading(high, recordedAt.plusSeconds(3600), 1000, 5.0, 5.0));
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void recompute_thenGetFleetSummary_returnsCoherentAggregates() throws Exception {
        mockMvc.perform(post("/api/fatigue/recompute"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/fleet/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAircraft").value(3))
                .andExpect(jsonPath("$.aircraftInAlert", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.averageFatigueIndex", greaterThan(0.0)))
                .andExpect(jsonPath("$.maxFatigueIndex", greaterThan(0.0)));
    }
}
