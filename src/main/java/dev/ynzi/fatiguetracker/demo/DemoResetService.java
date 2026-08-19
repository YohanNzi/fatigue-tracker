package dev.ynzi.fatiguetracker.demo;

import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import dev.ynzi.fatiguetracker.fatigue.FatigueService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

@Service
public class DemoResetService {

    private static final String DEMO_SEED = "db/migration/V5__seed_demo_data.sql";

    private final AircraftRepository aircraftRepository;
    private final FatigueService fatigueService;
    private final DataSource dataSource;
    private final TransactionTemplate transactionTemplate;

    public DemoResetService(AircraftRepository aircraftRepository,
                            FatigueService fatigueService,
                            DataSource dataSource,
                            PlatformTransactionManager transactionManager) {
        this.aircraftRepository = aircraftRepository;
        this.fatigueService = fatigueService;
        this.dataSource = dataSource;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Restaure les données relationnelles de démonstration puis recalcule leur fatigue.
     * Le recalcul reste volontairement hors de la transaction du reset : Spring Batch
     * refuse de démarrer lorsqu'une transaction est déjà active sur le thread appelant.
     */
    public int reset() {
        transactionTemplate.executeWithoutResult(status -> wipeAndSeed());
        fatigueService.recompute();
        return Math.toIntExact(aircraftRepository.count());
    }

    /** Supprime la flotte entière et rejoue l'unique source SQL du seed de démonstration. */
    @Transactional
    void wipeAndSeed() {
        aircraftRepository.deleteAllInBatch();
        new ResourceDatabasePopulator(new ClassPathResource(DEMO_SEED)).execute(dataSource);
    }
}
