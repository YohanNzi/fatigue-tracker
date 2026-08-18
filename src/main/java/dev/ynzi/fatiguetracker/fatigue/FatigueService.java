package dev.ynzi.fatiguetracker.fatigue;

import dev.ynzi.fatiguetracker.aircraft.AircraftRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FatigueService {

    private final JobLauncher jobLauncher;
    private final Job fatigueRecomputeJob;
    private final FatigueStatusRepository fatigueStatusRepository;
    private final AircraftRepository aircraftRepository;

    public FatigueService(JobLauncher jobLauncher, Job fatigueRecomputeJob,
                           FatigueStatusRepository fatigueStatusRepository,
                           AircraftRepository aircraftRepository) {
        this.jobLauncher = jobLauncher;
        this.fatigueRecomputeJob = fatigueRecomputeJob;
        this.fatigueStatusRepository = fatigueStatusRepository;
        this.aircraftRepository = aircraftRepository;
    }

    /**
     * Lance une exécution du job de recalcul de l'indice de fatigue de la flotte.
     * <p>
     * {@code NOT_SUPPORTED} (plutôt que d'hériter du {@code @Transactional(readOnly = true)}
     * de la classe) : {@code JobRepository} gère ses propres frontières transactionnelles à
     * chaque étape du job et refuse explicitement de démarrer si une transaction Spring est
     * déjà active sur le thread appelant ({@code IllegalStateException: Existing transaction
     * detected in JobRepository}) — bug latent découvert (J4, BDD) faute d'un test bout en
     * bout exerçant réellement cet endpoint contre le {@code FatigueService} non mocké.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public JobExecution recompute() {
        var jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        try {
            return jobLauncher.run(fatigueRecomputeJob, jobParameters);
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                 | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            throw new FatigueRecomputeException("Impossible de lancer le recalcul de fatigue : " + e.getMessage(), e);
        }
    }

    public Optional<FatigueStatus> findByAircraftId(Long aircraftId) {
        return fatigueStatusRepository.findByAircraftId(aircraftId);
    }

    public List<FatigueStatus> findAll() {
        return fatigueStatusRepository.findAllByOrderByAircraft_IdAsc();
    }

    /** Agrège les derniers statuts calculés sans assimiler les appareils non calculés à un indice nul. */
    public FleetSummary fleetSummary() {
        List<FatigueStatus> statuses = fatigueStatusRepository.findAll();
        int aircraftInAlert = (int) statuses.stream()
                .filter(FatigueStatus::isMaintenanceAlert)
                .count();
        double averageFatigueIndex = statuses.stream()
                .mapToDouble(FatigueStatus::getFatigueIndex)
                .average()
                .orElse(0.0);
        double maxFatigueIndex = statuses.stream()
                .mapToDouble(FatigueStatus::getFatigueIndex)
                .max()
                .orElse(0.0);

        return new FleetSummary(
                Math.toIntExact(aircraftRepository.count()),
                aircraftInAlert,
                averageFatigueIndex,
                maxFatigueIndex
        );
    }
}
