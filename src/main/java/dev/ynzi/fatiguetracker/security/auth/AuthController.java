package dev.ynzi.fatiguetracker.security.auth;

import dev.ynzi.fatiguetracker.security.auth.dto.LoginRequest;
import dev.ynzi.fatiguetracker.security.auth.dto.LoginResponse;
import dev.ynzi.fatiguetracker.security.jwt.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Émission de JWT (J3). {@code POST /api/auth/login} vérifie les identifiants (BCrypt,
 * voir {@link dev.ynzi.fatiguetracker.security.user.AppUserDetailsService}) via
 * {@link AuthenticationManager} et retourne un token signé si valides. Identifiants
 * invalides -&gt; {@link org.springframework.security.core.AuthenticationException},
 * traduite en 401 par {@code GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Auth", description = "Émission de JWT à partir des identifiants d'un compte de démo")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(summary = "S'authentifier",
            description = "Vérifie les identifiants et retourne un JWT avec le rôle et sa durée de validité.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie et JWT émis"),
            @ApiResponse(responseCode = "400", description = "Identifiants absents ou invalides"),
            @ApiResponse(responseCode = "401", description = "Identifiants incorrects")
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst("^ROLE_", ""))
                .orElseThrow(() -> new IllegalStateException("Utilisateur authentifié sans rôle : " + request.username()));

        String token = jwtService.generateToken(authentication.getName(), role);
        return LoginResponse.of(token, jwtService.expirationSeconds(), role);
    }
}
