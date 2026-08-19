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

import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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

    @MockBean
    private LoginRateLimiter loginRateLimiter;

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

    @Test
    void login_afterMaxAttemptsFailures_returns429() throws Exception {
        int maxAttempts = 5;
        AtomicInteger failures = new AtomicInteger();
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));
        doAnswer(invocation -> {
            failures.incrementAndGet();
            return null;
        }).when(loginRateLimiter).recordFailure("demo.maint");
        doAnswer(invocation -> {
            if (failures.get() >= maxAttempts) {
                throw new TooManyLoginAttemptsException("demo.maint");
            }
            return null;
        }).when(loginRateLimiter).checkNotBlocked("demo.maint");

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            performInvalidLogin().andExpect(status().isUnauthorized());
        }

        performInvalidLogin()
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.path").value("/api/auth/login"));
    }

    private org.springframework.test.web.servlet.ResultActions performInvalidLogin() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"username":"demo.maint","password":"wrong"}
                        """));
    }
}
