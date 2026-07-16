package dev.ynzi.fatiguetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FatigueTrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FatigueTrackerApplication.class, args);
    }
}
