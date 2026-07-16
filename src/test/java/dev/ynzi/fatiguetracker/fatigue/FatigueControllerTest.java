package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftNotFoundException;
import dev.ynzi.fatiguetracker.aircraft.AircraftService;
import dev.ynzi.fatiguetracker.security.RestAccessDeniedHandler;
import dev.ynzi.fatiguetracker.security.RestAuthenticationEntryPoint;
import dev.ynzi.fatiguetracker.security.SecurityConfig;
import dev.ynzi.fatiguetracker.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Voir {@link dev.ynzi.fatiguetracker.aircraft.AircraftControllerTest} pour le rationale de l'import. */
@WebMvcTest(FatigueController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class FatigueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FatigueService fatigueService;

    @MockBean
    private AircraftService aircraftService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "MAINT")
    void recompute_returnsJobExecutionSummary() throws Exception {
        JobExecution execution = new JobExecution(new JobInstance(1L, "fatigueRecomputeJob"), 42L, null);
        execution.setStatus(BatchStatus.COMPLETED);
        execution.setStartTime(LocalDateTime.of(2026, 1, 1, 10, 0));
        execution.setEndTime(LocalDateTime.of(2026, 1, 1, 10, 1));

        when(fatigueService.recompute()).thenReturn(execution);

        mockMvc.perform(post("/api/fatigue/recompute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").value(42))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void recompute_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/fatigue/recompute"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void recompute_withViewerRole_returns403() throws Exception {
        mockMvc.perform(post("/api/fatigue/recompute"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void getForAircraft_whenComputed_returnsStatus() throws Exception {
        Aircraft aircraft = new Aircraft("F-ABCD", "Mirage 2000", 1200.5);
        setId(aircraft, 1L);
        FatigueStatus fatigueStatus = new FatigueStatus(aircraft, 12.5, 3, Instant.parse("2026-01-01T00:00:00Z"), false);

        when(aircraftService.findById(1L)).thenReturn(aircraft);
        when(fatigueService.findByAircraftId(1L)).thenReturn(Optional.of(fatigueStatus));

        mockMvc.perform(get("/api/aircraft/{id}/fatigue", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aircraftId").value(1))
                .andExpect(jsonPath("$.fatigueIndex").value(12.5))
                .andExpect(jsonPath("$.computed").value(true))
                .andExpect(jsonPath("$.maintenanceAlert").value(false));
    }

    @Test
    void getForAircraft_whenNeverComputed_returnsNotComputedState() throws Exception {
        Aircraft aircraft = new Aircraft("F-ABCD", "Mirage 2000", 1200.5);
        setId(aircraft, 2L);

        when(aircraftService.findById(2L)).thenReturn(aircraft);
        when(fatigueService.findByAircraftId(2L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/aircraft/{id}/fatigue", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.computed").value(false))
                .andExpect(jsonPath("$.fatigueIndex").value(0.0));
    }

    @Test
    void getForAircraft_whenAircraftMissing_returns404() throws Exception {
        when(aircraftService.findById(99L)).thenThrow(new AircraftNotFoundException(99L));

        mockMvc.perform(get("/api/aircraft/{id}/fatigue", 99L))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFleet_returnsAllAndAlertsFilteredSeparately() throws Exception {
        Aircraft aircraft1 = new Aircraft("F-A1", "Rafale", 10.0);
        setId(aircraft1, 1L);
        Aircraft aircraft2 = new Aircraft("F-A2", "Rafale", 20.0);
        setId(aircraft2, 2L);

        FatigueStatus okStatus = new FatigueStatus(aircraft1, 5.0, 2, Instant.parse("2026-01-01T00:00:00Z"), false);
        FatigueStatus alertStatus = new FatigueStatus(aircraft2, 95.0, 5, Instant.parse("2026-01-01T00:00:00Z"), true);

        when(fatigueService.findAll()).thenReturn(List.of(okStatus, alertStatus));

        mockMvc.perform(get("/api/fatigue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aircraft.length()").value(2))
                .andExpect(jsonPath("$.maintenanceAlerts.length()").value(1))
                .andExpect(jsonPath("$.maintenanceAlerts[0].aircraftId").value(2));
    }

    private static void setId(Aircraft aircraft, Long id) throws Exception {
        var field = Aircraft.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(aircraft, id);
    }
}
