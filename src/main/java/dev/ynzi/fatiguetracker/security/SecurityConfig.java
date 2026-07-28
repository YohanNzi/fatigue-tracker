package dev.ynzi.fatiguetracker.security;

import dev.ynzi.fatiguetracker.security.jwt.JwtAuthenticationFilter;
import dev.ynzi.fatiguetracker.security.jwt.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Modèle d'autorisation J3 : API stateless, JWT signé HS256 (voir {@code security.jwt}).
 * <p>
 * <b>Lecture publique</b> : tous les {@code GET} de l'API, {@code /actuator/health},
 * {@code /api/auth/login} et la documentation API interactive (Swagger UI / OpenAPI, J4)
 * ne nécessitent aucune authentification. <b>Écriture protégée</b> :
 * toute <b>écriture de l'API</b> ({@code /api/**} en POST/PUT/DELETE : CRUD aircraft, ajout de
 * relevés, {@code POST /api/fatigue/recompute}) exige le rôle {@code MAINT} — {@code /api/**}
 * est ainsi secure-by-default (une future route d'écriture non listée reste protégée). Le reste
 * — front Angular servi depuis {@code classpath:/static} (J5.4), routes SPA, assets, Swagger,
 * {@code /actuator/health} — est public : ce ne sont que des ressources statiques ou de lecture,
 * la sécurité vit sur l'API.
 * <p>
 * Choix JWT stateless (plutôt que HTTP Basic) : pas de session serveur à maintenir, un
 * jeton auto-porteur (username + rôle) suffisant pour l'autorisation sans round-trip base à
 * chaque requête, expiration native, et un modèle qui s'étend naturellement vers un futur
 * front Angular (J5) sans renvoyer les identifiants à chaque appel.
 */
@Configuration
public class SecurityConfig {

    private final JwtService jwtService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final List<String> allowedOrigins;

    public SecurityConfig(JwtService jwtService,
                           RestAuthenticationEntryPoint authenticationEntryPoint,
                           RestAccessDeniedHandler accessDeniedHandler,
                           @Value("${app.cors.allowed-origins:http://localhost:4200}") List<String> allowedOrigins) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
                                                          PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                // CORS activé pour le front Angular (J5) servi sur un autre origin en dev
                // (ng serve : localhost:4200). Origins autorisés configurables via
                // app.cors.allowed-origins ; inutile si le front est servi par Spring
                // (même origin), mais indispensable dès qu'il tourne séparément.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Émission du jeton : publique.
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Lecture de l'API : publique.
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // Écriture de l'API : réservée au rôle MAINT (secure-by-default pour /api).
                        // Une future route /api/** non-GET est donc protégée par défaut.
                        .requestMatchers("/api/**").hasRole("MAINT")
                        // Tout le reste — front Angular (J5.4, servi depuis classpath:/static),
                        // routes SPA, assets, Swagger, /actuator/health — est public : ce ne sont
                        // que des ressources statiques/lecture ; la sécurité vit côté API ci-dessus.
                        .anyRequest().permitAll())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS pour le front (J5). Origins autorisés injectés depuis
     * {@code app.cors.allowed-origins} (liste séparée par des virgules, défaut
     * {@code http://localhost:4200}). On autorise l'en-tête {@code Authorization}
     * (Bearer JWT) et les méthodes de l'API ; pas de cookies donc
     * {@code allowCredentials} reste à false (le jeton voyage dans l'en-tête, pas
     * dans un cookie). Origins listés explicitement, jamais {@code *} en écriture.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Location"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
