package dev.ynzi.fatiguetracker.reading;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ynzi.fatiguetracker.AbstractIntegrationTest;
import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test d'intégration bout en bout (contrôleur -> service -> repository -> Postgres réel
 * via Testcontainers, migrations Flyway appliquées). Skippé proprement sans Docker.
 * <p>
 * {@code @Transactional} fait rejouer chaque test dans une transaction annulée en fin
 * de méthode : isolation entre tests sans réinitialiser tout le conteneur Postgres.
 * <p>
 * {@code @DirtiesContext(classMode = AFTER_CLASS)} (J3) : cette classe partage une
 * configuration Spring ({@code @SpringBootTest + @AutoConfigureMockMvc}) identique à
 * {@link dev.ynzi.fatiguetracker.security.SecurityIntegrationTest} — sans cette annotation,
 * le cache de contexte de Spring Test réutiliserait le même {@code ApplicationContext}
 * (et donc le même pool JDBC) pour les deux classes alors que chacune démarre/arrête son
 * propre conteneur Testcontainers, provoquant un pool connecté à un conteneur déjà arrêté.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class FlightReadingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AircraftRepository aircraftRepository;

    private Long aircraftId;

    @BeforeEach
    void setUp() {
        Aircraft aircraft = aircraftRepository.save(new Aircraft("F-IT01", "Rafale", 500.0));
        aircraftId = aircraft.getId();
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void create_thenList_persistsAndReturnsReading() throws Exception {
        String body = """
                {
                  "recordedAt": "2026-01-10T08:00:00Z",
                  "cycles": 4,
                  "maxLoadFactor": 2.1,
                  "flightHours": 3.5
                }
                """;

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", aircraftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(
                        "/api/aircraft/" + aircraftId + "/readings")))
                .andExpect(jsonPath("$.aircraftId").value(aircraftId))
                .andExpect(jsonPath("$.cycles").value(4));

        mockMvc.perform(get("/api/aircraft/{aircraftId}/readings", aircraftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].flightHours").value(3.5))
                .andExpect(jsonPath("$.page.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void create_forUnknownAircraft_returns404() throws Exception {
        String body = """
                {
                  "recordedAt": "2026-01-10T08:00:00Z",
                  "cycles": 4,
                  "maxLoadFactor": 2.1,
                  "flightHours": 3.5
                }
                """;

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", 999_999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_forUnknownAircraft_returns404() throws Exception {
        mockMvc.perform(get("/api/aircraft/{aircraftId}/readings", 999_999L))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void create_withInvalidBody_returns400() throws Exception {
        String invalidBody = """
                {
                  "recordedAt": null,
                  "cycles": -5,
                  "maxLoadFactor": 2.1,
                  "flightHours": -1.0
                }
                """;

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", aircraftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void create_alsoArchivesRawReadingWithMetadataInMongo() throws Exception {
        String body = """
                {
                  "recordedAt": "2026-03-01T08:00:00Z",
                  "cycles": 7,
                  "maxLoadFactor": 3.3,
                  "flightHours": 4.2,
                  "metadata": { "sensor": "S-42", "firmware": "1.4.0" }
                }
                """;

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", aircraftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // Le relevé brut a été archivé dans Mongo, avec ses métadonnées à schéma libre.
        mockMvc.perform(get("/api/aircraft/{aircraftId}/raw-readings", aircraftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].cycles").value(7))
                .andExpect(jsonPath("$.content[0].source").value("api"))
                .andExpect(jsonPath("$.content[0].metadata.sensor").value("S-42"))
                .andExpect(jsonPath("$.content[0].metadata.firmware").value("1.4.0"));
    }
}
