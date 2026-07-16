package dev.ynzi.fatiguetracker.aircraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ynzi.fatiguetracker.aircraft.dto.AircraftRequest;
import dev.ynzi.fatiguetracker.security.RestAccessDeniedHandler;
import dev.ynzi.fatiguetracker.security.RestAuthenticationEntryPoint;
import dev.ynzi.fatiguetracker.security.SecurityConfig;
import dev.ynzi.fatiguetracker.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @Import(SecurityConfig.class)} : la slice {@code @WebMvcTest} n'auto-détecte pas
 * notre {@code SecurityFilterChain} (bean de configuration hors du scan restreint de la
 * slice) — sans cet import, Spring Boot retomberait sur la sécurité par défaut (tout
 * authentifié, formulaire de login), pas sur nos règles réelles (lecture publique,
 * écriture {@code MAINT}). {@code JwtService}/{@code UserDetailsService} sont mockés
 * uniquement pour satisfaire l'injection de {@code SecurityConfig} — {@code @WithMockUser}
 * peuple le contexte de sécurité en amont du filtre JWT, qui n'est donc jamais sollicité ici.
 */
@WebMvcTest(AircraftController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AircraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AircraftService aircraftService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "MAINT")
    void create_withValidBody_returns201AndLocation() throws Exception {
        AircraftRequest request = new AircraftRequest("F-ABCD", "Mirage 2000", 1200.5);
        Aircraft saved = new Aircraft("F-ABCD", "Mirage 2000", 1200.5);
        setId(saved, 1L);

        when(aircraftService.create(any(AircraftRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/aircraft/1")))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.registration").value("F-ABCD"))
                .andExpect(jsonPath("$.model").value("Mirage 2000"));
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void create_withBlankRegistration_returns400WithFieldErrors() throws Exception {
        AircraftRequest invalidRequest = new AircraftRequest("", "Mirage 2000", 10.0);

        mockMvc.perform(post("/api/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("registration"));
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        AircraftRequest request = new AircraftRequest("F-ABCD", "Mirage 2000", 1200.5);

        mockMvc.perform(post("/api/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void create_withViewerRole_returns403() throws Exception {
        AircraftRequest request = new AircraftRequest("F-ABCD", "Mirage 2000", 1200.5);

        mockMvc.perform(post("/api/aircraft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void findById_whenMissing_returns404() throws Exception {
        when(aircraftService.findById(eq(99L))).thenThrow(new AircraftNotFoundException(99L));

        mockMvc.perform(get("/api/aircraft/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Aircraft introuvable pour l'id 99"));
    }

    @Test
    void findAll_withoutAuth_returns200() throws Exception {
        // Lecture publique : aucune authentification requise (voir SecurityConfig).
        mockMvc.perform(get("/api/aircraft"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void delete_whenMissing_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new AircraftNotFoundException(42L)).when(aircraftService).delete(42L);

        mockMvc.perform(delete("/api/aircraft/{id}", 42L))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_withoutAuth_returns401() throws Exception {
        mockMvc.perform(delete("/api/aircraft/{id}", 42L))
                .andExpect(status().isUnauthorized());
    }

    private static void setId(Aircraft aircraft, Long id) throws Exception {
        var field = Aircraft.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(aircraft, id);
    }
}
