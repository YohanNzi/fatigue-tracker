package dev.ynzi.fatiguetracker;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * Point d'entrée JUnit Platform Suite (J4, BDD) qui délègue au moteur Cucumber : découvre
 * les {@code .feature} sous {@code src/test/resources/features} et exécute les steps du
 * package {@code dev.ynzi.fatiguetracker.cucumber}. Nommée {@code CucumberTest} pour être
 * ramassée par les patterns par défaut de Surefire/Failsafe (comme n'importe quelle autre
 * classe {@code *Test}), donc lancée automatiquement par {@code ./mvnw test}/{@code verify}
 * aux côtés du reste de la suite — aucune configuration Maven dédiée requise.
 * <p>
 * Voir {@link dev.ynzi.fatiguetracker.cucumber.CucumberSpringConfiguration} pour le contexte
 * Spring partagé par les scénarios (Postgres réel via Testcontainers) : ces scénarios
 * nécessitent donc Docker, comme le reste de la suite d'intégration (voir README).
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "dev.ynzi.fatiguetracker.cucumber")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class CucumberTest {
}
