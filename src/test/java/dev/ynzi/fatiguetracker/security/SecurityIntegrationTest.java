package dev.ynzi.fatiguetracker.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ynzi.fatiguetracker.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bout-en-bout, Postgres réel via Testcontainers (migrations Flyway comprises, dont
 * {@code V4__app_user.sql} qui seed les deux comptes de démo) : flow complet
 * login -> JWT -> appel protégé, et confirmation des 401/403/200 selon rôle sur un
 * vrai passage par le {@code SecurityFilterChain} (pas une slice {@code @WebMvcTest}).
 * Skippé proprement sans Docker (voir {@link AbstractIntegrationTest}).
 * <p>
 * {@code @DirtiesContext(classMode = AFTER_CLASS)} : voir le rationale détaillé sur
 * {@link dev.ynzi.fatiguetracker.reading.FlightReadingIntegrationTest} — cette classe
 * partage une configuration Spring identique (même combinaison d'annotations), le cache
 * de contexte de Spring Test doit donc être invalidé explicitement entre les deux pour
 * éviter un pool JDBC connecté au conteneur Testcontainers (déjà arrêté) de l'autre classe.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void login_withDemoMaintAccount_thenCreateAircraft_succeeds() throws Exception {
        String token = login("demo.maint", "maint123");

        mockMvc.perform(post("/api/aircraft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"registration":"F-SEC01","model":"Rafale","flightHours":10.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.registration").value("F-SEC01"));
    }

    @Test
    void login_withDemoViewerAccount_thenCreateAircraft_returns403() throws Exception {
        String token = login("demo.viewer", "viewer123");

        mockMvc.perform(post("/api/aircraft")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"registration":"F-SEC02","model":"Rafale","flightHours":10.0}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAircraft_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"registration":"F-SEC03","model":"Rafale","flightHours":10.0}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_withWrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo.maint","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listAircraft_withoutToken_returns200() throws Exception {
        // Lecture publique : aucune authentification requise, y compris contre le vrai filtre.
        mockMvc.perform(get("/api/aircraft"))
                .andExpect(status().isOk());
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = body.get("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
