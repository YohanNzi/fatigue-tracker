package dev.ynzi.fatiguetracker.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Métadonnées de la documentation API interactive (J4, springdoc-openapi). Expose
 * {@code /v3/api-docs} et {@code /swagger-ui.html} — routes rendues publiques dans
 * {@link dev.ynzi.fatiguetracker.security.SecurityConfig}, sans quoi Swagger UI serait
 * inaccessible derrière l'authentification.
 * <p>
 * Le schéma de sécurité {@code bearerAuth} documente uniquement le format attendu de
 * l'en-tête {@code Authorization} pour les endpoints protégés (rôle {@code MAINT}) ; il
 * ne modifie aucune règle d'autorisation réelle, portée exclusivement par
 * {@code SecurityFilterChain}.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fatigueTrackerOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FatigueTracker API")
                        .description("Suivi de fatigue structurelle d'une flotte d'appareils — CRUD "
                                + "aircraft/reading, recalcul de l'indice de fatigue (Spring Batch) et "
                                + "authentification JWT. Exercice de portfolio technique, formule de "
                                + "fatigue illustrative (voir README).")
                        .version("v0.1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
