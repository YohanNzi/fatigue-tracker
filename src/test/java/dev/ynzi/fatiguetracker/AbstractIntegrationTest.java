package dev.ynzi.fatiguetracker;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base pour les tests d'intégration nécessitant les vraies bases : Postgres (relationnel,
 * source de vérité) et MongoDB (relevés bruts, J5.5).
 * <p>
 * {@code disabledWithoutDocker = true} fait que ces tests sont proprement
 * <b>skippés</b> (et non en échec) si aucun daemon Docker n'est disponible dans
 * l'environnement d'exécution — cf. README pour le statut réel de ces tests.
 */
@Testcontainers(disabledWithoutDocker = true)
public abstract class AbstractIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fatiguetracker-test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }
}
