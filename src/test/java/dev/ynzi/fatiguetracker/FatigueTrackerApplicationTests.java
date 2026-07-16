package dev.ynzi.fatiguetracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Démarre le contexte complet contre un vrai Postgres (Testcontainers) et valide
 * au passage que les migrations Flyway correspondent au mapping JPA
 * ({@code ddl-auto: validate}). Skippé proprement sans Docker (voir
 * {@link AbstractIntegrationTest}).
 */
@SpringBootTest
class FatigueTrackerApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Le contexte Spring doit démarrer sans erreur, migrations Flyway comprises.
    }
}
