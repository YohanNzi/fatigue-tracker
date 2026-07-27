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
 * toute autre requête (CRUD aircraft, ajout de relevés, {@code POST /api/fatigue/recompute})
 * exige le rôle {@code MAINT} — {@code anyRequest().hasRole("MAINT")} est volontairement le
 * cas par défaut : une route future non explicitement listée en lecture publique sera donc
 * protégée par défaut plutôt que de fuiter accidentellement en écriture libre.
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
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        // Documentation API interactive (J4) : Swagger UI + /v3/api-docs.
                        // Publique par nature (documente une API déjà publique en lecture),
                        // et doit rester accessible sans jeton sous peine d'être inutilisable.
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()
                        .anyRequest().hasRole("MAINT"))
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
