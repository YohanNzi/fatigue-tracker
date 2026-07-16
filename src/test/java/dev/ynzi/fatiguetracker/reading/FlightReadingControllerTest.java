package dev.ynzi.fatiguetracker.reading;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftNotFoundException;
import dev.ynzi.fatiguetracker.reading.dto.FlightReadingRequest;
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

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Voir {@link dev.ynzi.fatiguetracker.aircraft.AircraftControllerTest} pour le rationale de l'import. */
@WebMvcTest(FlightReadingController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class FlightReadingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FlightReadingService flightReadingService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "MAINT")
    void create_withValidBody_returns201() throws Exception {
        FlightReadingRequest request = new FlightReadingRequest(Instant.parse("2026-01-01T10:00:00Z"), 3, 1.8, 2.5);
        Aircraft aircraft = new Aircraft("F-ABCD", "Mirage 2000", 1200.5);
        setId(aircraft, 1L);
        FlightReading saved = new FlightReading(aircraft, request.recordedAt(), request.cycles(),
                request.maxLoadFactor(), request.flightHours());
        setId(saved, 10L);

        when(flightReadingService.create(eq(1L), any(FlightReadingRequest.class))).thenReturn(saved);

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.aircraftId").value(1))
                .andExpect(jsonPath("$.cycles").value(3))
                .andExpect(jsonPath("$.maxLoadFactor").value(1.8));
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void create_forUnknownAircraft_returns404() throws Exception {
        FlightReadingRequest request = new FlightReadingRequest(Instant.parse("2026-01-01T10:00:00Z"), 3, 1.8, 2.5);

        when(flightReadingService.create(eq(99L), any(FlightReadingRequest.class)))
                .thenThrow(new AircraftNotFoundException(99L));

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @WithMockUser(roles = "MAINT")
    void create_withNegativeCycles_returns400WithFieldErrors() throws Exception {
        FlightReadingRequest invalidRequest = new FlightReadingRequest(Instant.parse("2026-01-01T10:00:00Z"), -1, 1.8, 2.5);

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cycles"));
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        FlightReadingRequest request = new FlightReadingRequest(Instant.parse("2026-01-01T10:00:00Z"), 3, 1.8, 2.5);

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void create_withViewerRole_returns403() throws Exception {
        FlightReadingRequest request = new FlightReadingRequest(Instant.parse("2026-01-01T10:00:00Z"), 3, 1.8, 2.5);

        mockMvc.perform(post("/api/aircraft/{aircraftId}/readings", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void findByAircraft_returnsList() throws Exception {
        Aircraft aircraft = new Aircraft("F-ABCD", "Mirage 2000", 1200.5);
        setId(aircraft, 1L);
        FlightReading reading = new FlightReading(aircraft, Instant.parse("2026-01-01T10:00:00Z"), 3, 1.8, 2.5);
        setId(reading, 10L);

        when(flightReadingService.findByAircraft(1L)).thenReturn(List.of(reading));

        mockMvc.perform(get("/api/aircraft/{aircraftId}/readings", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].aircraftId").value(1));
    }

    @Test
    void findByAircraft_whenAircraftMissing_returns404() throws Exception {
        when(flightReadingService.findByAircraft(99L)).thenThrow(new AircraftNotFoundException(99L));

        mockMvc.perform(get("/api/aircraft/{aircraftId}/readings", 99L))
                .andExpect(status().isNotFound());
    }

    private static void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}
