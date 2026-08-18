package dev.ynzi.fatiguetracker.fatigue;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres de la planification périodique du recalcul de fatigue.
 *
 * @param enabled active ou non le déclenchement automatique du job
 * @param cron    expression cron Spring définissant la fréquence de recalcul
 */
@ConfigurationProperties(prefix = "fatigue.schedule")
public record FatigueScheduleProperties(boolean enabled, String cron) {
}
