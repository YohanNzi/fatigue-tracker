package dev.ynzi.fatiguetracker.fatigue.batch;

import dev.ynzi.fatiguetracker.aircraft.Aircraft;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatus;
import dev.ynzi.fatiguetracker.fatigue.FatigueStatusRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.batch.item.Chunk;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Upsert par chunk en une seule lecture des statuts existants (pas de N+1). */
class FatigueStatusWriterTest {

    private static final Instant NOW = Instant.parse("2026-09-29T12:00:00Z");

    private final FatigueStatusRepository repository = mock(FatigueStatusRepository.class);
    private final FatigueStatusWriter writer = new FatigueStatusWriter(repository);

    @Test
    @SuppressWarnings("unchecked")
    void write_updatesExistingAndInsertsNew_withSingleLookupQuery() throws Exception {
        Aircraft known = aircraft(1L, "F-OLD1");
        Aircraft fresh = aircraft(2L, "F-NEW1");
        FatigueStatus existing = new FatigueStatus(known, 10.0, 1, NOW.minusSeconds(3600), false);
        FatigueStatus recomputedKnown = new FatigueStatus(known, 90.0, 3, NOW, true);
        FatigueStatus computedFresh = new FatigueStatus(fresh, 5.0, 1, NOW, false);

        when(repository.findByAircraftIdIn(anyCollection())).thenReturn(List.of(existing));

        writer.write(new Chunk<>(List.of(recomputedKnown, computedFresh)));

        verify(repository, times(1)).findByAircraftIdIn(List.of(1L, 2L));
        verify(repository, never()).findByAircraftId(anyLong());

        ArgumentCaptor<List<FatigueStatus>> saved = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(existing, computedFresh);
        // La ligne existante est mise à jour en place (identité conservée), pas dupliquée.
        assertThat(existing.getFatigueIndex()).isEqualTo(90.0);
        assertThat(existing.isMaintenanceAlert()).isTrue();
        assertThat(existing.getComputedAt()).isEqualTo(NOW);
    }

    private static Aircraft aircraft(Long id, String registration) throws Exception {
        Aircraft aircraft = new Aircraft(registration, "A320", 100.0);
        Field idField = Aircraft.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(aircraft, id);
        return aircraft;
    }
}
