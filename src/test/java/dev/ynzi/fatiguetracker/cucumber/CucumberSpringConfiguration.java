package dev.ynzi.fatiguetracker.cucumber;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Contexte Spring partagé par tous les scénarios Cucumber (J4, BDD), Postgres réel via
 * Testcontainers (migrations Flyway comprises, dont {@code V4__app_user.sql} qui seed
 * les comptes de démo utilisés par {@code auth.feature}).
 * <p>
 * Contrairement à {@link dev.ynzi.fatiguetracker.AbstractIntegrationTest} (utilisée par les
 * autres tests d'intégration du projet), cette classe <b>ne</b> porte <b>pas</b>
 * {@code @Testcontainers}/{@code @Container} : cucumber-spring construit le contexte Spring
 * via un {@code TestContextManager} instancié directement, en dehors du cycle de vie des
 * extensions JUnit 5 sur lequel repose l'extension {@code @Testcontainers} — celle-ci ne
 * démarrerait donc jamais le conteneur ici. Le conteneur est par conséquent démarré
 * explicitement ({@code start()}), et {@code @DynamicPropertySource} reste fonctionnel :
 * ce mécanisme est traité par le framework Spring TestContext lui-même (indépendamment du
 * test runner), pas par une extension JUnit 5.
 * <p>
 * <b>Conséquence assumée</b> : ces scénarios BDD nécessitent Docker et ne sont <b>pas</b>
 * skippés proprement en son absence (contrairement au reste de la suite d'intégration) —
 * acceptable ici car les runners CI (GitHub Actions {@code ubuntu-latest}) embarquent
 * toujours un daemon Docker actif. Voir le README, section Tests & couverture.
 */
@CucumberContextConfiguration
@SpringBootTest
@AutoConfigureMockMvc
public class CucumberSpringConfiguration {

    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("fatiguetracker-bdd")
            .withUsername("test")
            .withPassword("test");

    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    static {
        POSTGRES.start();
        MONGO.start();
    }

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.mongodb.uri", MONGO::getReplicaSetUrl);
    }
}
