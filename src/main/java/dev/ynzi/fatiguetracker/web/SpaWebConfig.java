package dev.ynzi.fatiguetracker.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Sert le front Angular (J5.4) empaqueté dans {@code classpath:/static/} par le profil
 * Maven {@code fullstack}, sur la même origine que l'API — un seul jar, une seule URL.
 * <p>
 * Fallback SPA : une route Angular côté client (ex. {@code /aircraft/1} en accès direct
 * ou rechargement) n'existe pas côté serveur → on renvoie {@code index.html} pour laisser
 * le routeur Angular reprendre la main. Les chemins d'API/outillage ({@code /api},
 * {@code /actuator}, Swagger) et les assets manquants (chemin avec extension) ne sont
 * jamais réécrits : ils suivent leur traitement normal (contrôleur ou 404).
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws java.io.IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }
                        // Chemins back / outillage : pas de réécriture (traitement normal).
                        if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator")
                                || resourcePath.startsWith("swagger-ui") || resourcePath.startsWith("v3/")) {
                            return null;
                        }
                        // Asset manquant (a une extension) → 404 ; route SPA → index.html.
                        if (resourcePath.contains(".")) {
                            return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
