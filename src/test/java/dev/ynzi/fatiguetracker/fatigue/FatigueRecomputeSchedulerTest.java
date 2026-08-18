package dev.ynzi.fatiguetracker.fatigue;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Vérifie le déclenchement du scheduler sans dépendre du timing de {@code @Scheduled}. */
class FatigueRecomputeSchedulerTest {

    @Test
    void recomputeFatigue_callsFatigueService() {
        FatigueService fatigueService = mock(FatigueService.class);
        JobExecution jobExecution = mock(JobExecution.class);
        when(fatigueService.recompute()).thenReturn(jobExecution);
        FatigueRecomputeScheduler scheduler = new FatigueRecomputeScheduler(fatigueService);

        scheduler.recomputeFatigue();

        verify(fatigueService).recompute();
    }

    @Test
    void recomputeFatigue_whenServiceFails_swallowsException() {
        FatigueService fatigueService = mock(FatigueService.class);
        doThrow(new FatigueRecomputeException("Échec du job", new RuntimeException()))
                .when(fatigueService).recompute();
        FatigueRecomputeScheduler scheduler = new FatigueRecomputeScheduler(fatigueService);

        assertThatCode(scheduler::recomputeFatigue).doesNotThrowAnyException();

        verify(fatigueService).recompute();
    }
}
