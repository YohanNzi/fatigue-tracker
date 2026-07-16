package dev.ynzi.fatiguetracker.cucumber;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ynzi.fatiguetracker.fatigue.FatigueProperties;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Steps du scénario métier clé (J4, BDD) : ingestion de relevés -> recalcul Spring Batch
 * -> alerte de maintenance (voir {@code features/fatigue_alert.feature}). Bout en bout via
 * MockMvc contre l'application réelle (Postgres Testcontainers, voir
 * {@link CucumberSpringConfiguration}) : mêmes endpoints/JWT que la vraie API, aucun mock.
 * <p>
 * Une instance de cette classe est créée par Cucumber pour chaque scénario : les champs
 * d'instance ({@code aircraftIdsByRegistration}, {@code maintToken}) sont donc naturellement
 * isolés d'un scénario à l'autre, sans nécessiter de bean Spring à portée dédiée.
 */
public class FatigueAlertSteps {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final FatigueProperties fatigueProperties;

    private final Map<String, Long> aircraftIdsByRegistration = new HashMap<>();
    private String maintToken;

    public FatigueAlertSteps(MockMvc mockMvc, ObjectMapper objectMapper, FatigueProperties fatigueProperties) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.fatigueProperties = fatigueProperties;
    }

    @Given("un appareil {string} fraîchement enregistré, sans aucun relevé")
    public void registerFreshAircraft(String registration) throws Exception {
        String body = """
                {"registration":"%s","model":"Rafale","flightHours":0.0}
                """.formatted(registration);

        MvcResult result = mockMvc.perform(post("/api/aircraft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + maintToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        aircraftIdsByRegistration.put(registration, json.get("id").asLong());
    }

    @When("des relevés de vol fortement sollicités sont ingérés pour l'appareil {string}")
    public void ingestHeavyReadings(String registration) throws Exception {
        long aircraftId = aircraftId(registration);
        // Cycles et facteur de charge élevés : franchit largement fatigue.alert-threshold
        // (mêmes ordres de grandeur que FatigueBatchJobIntegrationTest côté JUnit).
        String[] bodies = {
                """
                {"recordedAt":"2026-01-01T08:00:00Z","cycles":1000,"maxLoadFactor":5.0,"flightHours":5.0}
                """,
                """
                {"recordedAt":"2026-01-02T08:00:00Z","cycles":1000,"maxLoadFactor":5.0,"flightHours":5.0}
                """
        };
        for (String body : bodies) {
            mockMvc.perform(post("/api/aircraft/{id}/readings", aircraftId)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + maintToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated());
        }
    }

    @When("un relevé de vol léger est ingéré pour l'appareil {string}")
    public void ingestLightReading(String registration) throws Exception {
        long aircraftId = aircraftId(registration);
        String body = """
                {"recordedAt":"2026-01-01T08:00:00Z","cycles":2,"maxLoadFactor":1.0,"flightHours":1.0}
                """;

        mockMvc.perform(post("/api/aircraft/{id}/readings", aircraftId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + maintToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @And("le recalcul de l'indice de fatigue de la flotte est déclenché par un compte MAINT")
    public void triggerRecompute() throws Exception {
        mockMvc.perform(post("/api/fatigue/recompute")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + maintToken()))
                .andExpect(status().isOk());
    }

    @Then("l'appareil {string} est signalé en alerte de maintenance")
    public void assertMaintenanceAlert(String registration) throws Exception {
        fatigueStatusOf(registration)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceAlert").value(true));
    }

    @Then("l'appareil {string} n'est pas en alerte de maintenance")
    public void assertNoMaintenanceAlert(String registration) throws Exception {
        fatigueStatusOf(registration)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maintenanceAlert").value(false));
    }

    @And("l'indice de fatigue de l'appareil {string} dépasse le seuil configuré")
    public void assertIndexAboveThreshold(String registration) throws Exception {
        MvcResult result = fatigueStatusOf(registration)
                .andExpect(status().isOk())
                .andReturn();

        double index = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("fatigueIndex").asDouble();
        assertThat(index).isGreaterThan(fatigueProperties.alertThreshold());
    }

    private ResultActions fatigueStatusOf(String registration) throws Exception {
        return mockMvc.perform(get("/api/aircraft/{id}/fatigue", aircraftId(registration)));
    }

    private long aircraftId(String registration) {
        Long id = aircraftIdsByRegistration.get(registration);
        if (id == null) {
            throw new IllegalStateException("Aucun appareil enregistré dans ce scénario pour : " + registration);
        }
        return id;
    }

    /** Jeton MAINT mémoïsé pour la durée du scénario (login demo.maint via l'API réelle). */
    private String maintToken() throws Exception {
        if (maintToken == null) {
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"demo.maint","password":"maint123"}
                                    """))
                    .andExpect(status().isOk())
                    .andReturn();
            maintToken = objectMapper.readTree(result.getResponse().getContentAsString())
                    .get("accessToken").asText();
        }
        return maintToken;
    }
}
