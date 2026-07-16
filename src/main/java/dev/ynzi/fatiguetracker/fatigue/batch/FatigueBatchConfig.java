package dev.ynzi.fatiguetracker.fatigue.batch;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import dev.ynzi.fatiguetracker.fatigue.FatigueProperties;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Map;

/**
 * Configuration du job Spring Batch idiomatique (chunk-oriented) de recalcul de
 * l'indice de fatigue de la flotte : lit tous les appareils, calcule leur indice
 * ({@link AircraftFatigueProcessor}) et upsert le résultat ({@link FatigueStatusWriter}).
 * <p>
 * Déclenché uniquement à la demande via {@code POST /api/fatigue/recompute}
 * ({@code spring.batch.job.enabled=false} : pas d'exécution automatique au démarrage).
 */
@Configuration
public class FatigueBatchConfig {

    public static final String JOB_NAME = "fatigueRecomputeJob";
    static final String STEP_NAME = "recomputeFatigueStep";

    @Bean
    public Job fatigueRecomputeJob(JobRepository jobRepository, Step recomputeFatigueStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(recomputeFatigueStep)
                .build();
    }

    @Bean
    public Step recomputeFatigueStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      RepositoryItemReader<Aircraft> aircraftReader,
                                      AircraftFatigueProcessor aircraftFatigueProcessor,
                                      FatigueStatusWriter fatigueStatusWriter,
                                      FatigueProperties fatigueProperties) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Aircraft, FatigueStatus>chunk(fatigueProperties.chunkSize(), transactionManager)
                .reader(aircraftReader)
                .processor(aircraftFatigueProcessor)
                .writer(fatigueStatusWriter)
                .build();
    }

    @Bean
    public RepositoryItemReader<Aircraft> aircraftReader(AircraftRepository aircraftRepository,
                                                          FatigueProperties fatigueProperties) {
        RepositoryItemReader<Aircraft> reader = new RepositoryItemReader<>();
        reader.setRepository(aircraftRepository);
        reader.setMethodName("findAll");
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        reader.setPageSize(fatigueProperties.chunkSize());
        reader.setName("aircraftReader");
        return reader;
    }
}
