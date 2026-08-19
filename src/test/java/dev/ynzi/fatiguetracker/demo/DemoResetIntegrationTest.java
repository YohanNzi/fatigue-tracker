package dev.ynzi.fatiguetracker.demo;

import dev.ynzi.fatiguetracker.AbstractIntegrationTest;
import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test bout en bout du reset contre Postgres et le vrai job Spring Batch. */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DemoResetIntegrationTest extends AbstractIntegrationTest {

    private static final Set<String> DEMO_REGISTRATIONS =
            Set.of("F-GKXA", "F-GKXB", "F-GKXC", "F-GKXD", "F-GKXE");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AircraftRepository aircraftRepository;

    @Autowired
    private FatigueStatusRepository fatigueStatusRepository;

    @Test
    @WithMockUser(roles = "MAINT")
    void reset_restoresExactlyTheDemoFleetAndRecomputesFatigue() throws Exception {
        aircraftRepository.saveAndFlush(new Aircraft("F-EXTRA", "Avion hors démonstration", 10.0));

        mockMvc.perform(post("/api/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aircraftSeeded").value(5));

        Set<String> registrations = aircraftRepository.findAll().stream()
                .map(Aircraft::getRegistration)
                .collect(Collectors.toSet());

        assertThat(aircraftRepository.count()).isEqualTo(5);
        assertThat(registrations).isEqualTo(DEMO_REGISTRATIONS);
        assertThat(fatigueStatusRepository.findAll())
                .hasSize(5)
                .anyMatch(status -> status.isMaintenanceAlert());
    }
}
