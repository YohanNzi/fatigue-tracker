package dev.ynzi.fatiguetracker.security;

import dev.ynzi.fatiguetracker.security.jwt.JwtAuthenticationFilter;
import dev.ynzi.fatiguetracker.security.jwt.JwtService;
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

    public SecurityConfig(JwtService jwtService,
                           RestAuthenticationEntryPoint authenticationEntryPoint,
                           RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtService = jwtService;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
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
}
