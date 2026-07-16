package dev.ynzi.fatiguetracker.security.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /api/auth/login} isolé (AuthenticationManager mocké) : émission du JWT sur
 * identifiants valides, 401 sur identifiants invalides, 400 sur corps invalide. Le flow
 * bout-en-bout réel (login -> token -> appel protégé, contre la table {@code app_user}
 * seedée) est couvert par {@link dev.ynzi.fatiguetracker.security.SecurityIntegrationTest}.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @Test
    void login_withValidCredentials_returnsToken() throws Exception {
        var authentication = new UsernamePasswordAuthenticationToken(
                "demo.maint", null, java.util.List.of(new SimpleGrantedAuthority("ROLE_MAINT")));
        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken("demo.maint", "MAINT")).thenReturn("fake.jwt.token");
        when(jwtService.expirationSeconds()).thenReturn(3600L);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo.maint","password":"maint123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("fake.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600))
                .andExpect(jsonPath("$.role").value("MAINT"));
    }

    @Test
    void login_withInvalidCredentials_returns401() throws Exception {
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"demo.maint","password":"wrong"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_withBlankBody_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","password":""}
                                """))
                .andExpect(status().isBadRequest());
    }
}
