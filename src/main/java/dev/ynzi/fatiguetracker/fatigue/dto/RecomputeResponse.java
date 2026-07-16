package dev.ynzi.fatiguetracker.fatigue.dto;

import org.springframework.batch.core.JobExecution;

import java.time.LocalDateTime;

/** Résumé d'une exécution du job {@code fatigueRecomputeJob}. */
public record RecomputeResponse(
        Long jobExecutionId,
        String status,
        LocalDateTime startTime,
        LocalDateTime endTime,
        long aircraftProcessed
) {

    public static RecomputeResponse from(JobExecution execution) {
        long processed = execution.getStepExecutions().stream()
                .mapToLong(stepExecution -> stepExecution.getWriteCount())
                .sum();

        return new RecomputeResponse(
                execution.getId(),
                execution.getStatus().toString(),
                execution.getStartTime(),
                execution.getEndTime(),
                processed
        );
    }
}
