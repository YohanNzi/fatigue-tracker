package dev.ynzi.fatiguetracker.aircraft;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ynzi.fatiguetracker.aircraft.dto.AircraftRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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

@WebMvcTest(AircraftController.class)
class AircraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AircraftService aircraftService;

    @Test
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
    void findById_whenMissing_returns404() throws Exception {
        when(aircraftService.findById(eq(99L))).thenThrow(new AircraftNotFoundException(99L));

        mockMvc.perform(get("/api/aircraft/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Aircraft introuvable pour l'id 99"));
    }

    @Test
    void delete_whenMissing_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new AircraftNotFoundException(42L)).when(aircraftService).delete(42L);

        mockMvc.perform(delete("/api/aircraft/{id}", 42L))
                .andExpect(status().isNotFound());
    }

    private static void setId(Aircraft aircraft, Long id) throws Exception {
        var field = Aircraft.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(aircraft, id);
    }
}
