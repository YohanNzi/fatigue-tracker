package dev.ynzi.fatiguetracker.cucumber;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Steps du scénario métier clé (J4, BDD) : login -> JWT -> écriture autorisée/refusée selon
 * le rôle (voir {@code features/auth.feature}). Bout en bout via MockMvc contre l'application
 * réelle (Postgres Testcontainers, comptes de démo seedés par {@code V4__app_user.sql}, voir
 * {@link CucumberSpringConfiguration}) : même flow que {@code SecurityIntegrationTest}, en
 * Gherkin plutôt qu'en JUnit pur.
 */
public class AuthSteps {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    private String currentToken;
    private MvcResult lastResult;

    public AuthSteps(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Given("l'utilisateur se connecte avec le compte de démo {string} et le mot de passe {string}")
    public void login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        currentToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken").asText();
        assertThat(currentToken).isNotBlank();
    }

    @When("il crée l'appareil {string} avec son jeton")
    public void createAircraftWithToken(String registration) throws Exception {
        lastResult = mockMvc.perform(post("/api/aircraft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + currentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"registration":"%s","model":"Rafale","flightHours":1.0}
                                """.formatted(registration)))
                .andReturn();
    }

    @When("il crée l'appareil {string} sans jeton")
    public void createAircraftWithoutToken(String registration) throws Exception {
        lastResult = mockMvc.perform(post("/api/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"registration":"%s","model":"Rafale","flightHours":1.0}
                                """.formatted(registration)))
                .andReturn();
    }

    @Then("la création de l'appareil {string} réussit")
    public void assertCreated(String registration) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(201);
    }

    @Then("la création de l'appareil {string} est refusée avec le statut {int}")
    public void assertRejected(String registration, int expectedStatus) {
        assertThat(lastResult.getResponse().getStatus()).isEqualTo(expectedStatus);
    }
}
