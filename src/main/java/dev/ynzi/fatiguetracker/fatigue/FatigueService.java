package dev.ynzi.fatiguetracker.fatigue;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class FatigueService {

    private final JobLauncher jobLauncher;
    private final Job fatigueRecomputeJob;
    private final FatigueStatusRepository fatigueStatusRepository;

    public FatigueService(JobLauncher jobLauncher, Job fatigueRecomputeJob,
                           FatigueStatusRepository fatigueStatusRepository) {
        this.jobLauncher = jobLauncher;
        this.fatigueRecomputeJob = fatigueRecomputeJob;
        this.fatigueStatusRepository = fatigueStatusRepository;
    }

    /** Lance une exécution du job de recalcul de l'indice de fatigue de la flotte. */
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
}
