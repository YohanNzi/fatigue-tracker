package dev.ynzi.fatiguetracker.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtre stateless : lit l'en-tête {@code Authorization: Bearer <token>}, valide le
 * JWT et peuple le {@link SecurityContextHolder} à partir de ses claims (subject +
 * rôle) — aucun accès base nécessaire à chaque requête, le token porte tout ce qu'il
 * faut pour l'autorisation.
 * <p>
 * Un token absent, malformé, expiré ou mal signé n'est <b>pas</b> une erreur ici : la
 * requête continue simplement non authentifiée, et c'est {@link org.springframework.security.web.SecurityFilterChain}
 * (routes {@code permitAll}/{@code hasRole}) qui décide ensuite si elle est acceptée
 * (401 via {@link RestAuthenticationEntryPoint} si l'authentification était requise).
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                Claims claims = jwtService.parseAndValidate(token);
                String username = jwtService.extractUsername(claims);
                String role = jwtService.extractRole(claims);

                var authentication = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException ex) {
                logger.debug("JWT invalide ignoré : " + ex.getMessage());
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
