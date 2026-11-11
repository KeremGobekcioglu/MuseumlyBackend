package com.kg.museumly.backend.crawler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class MetEnrichmentWorkerTest
{
    private MetCandidatePicker picker;
    private MetObjectClient client;
    private MetEnrichmentWriter writer;
    private MetEnrichmentWorker worker;

    private final MetCandidate a = new MetCandidate(1L, "100", "Egyptian Art");
    private final MetCandidate b = new MetCandidate(2L, "200", "Islamic Art");

    @BeforeEach
    void setUp() {
        picker = mock(MetCandidatePicker.class);
        client = mock(MetObjectClient.class);
        writer = mock(MetEnrichmentWriter.class);
        worker = new MetEnrichmentWorker(2, 0L ,picker, client, writer);   // pacing 0
    }

    @Test
    void nothingToPickPausesWithoutCallingTheMet() {
        when(picker.pickNext(2)).thenReturn(List.of());

        worker.tick();
        worker.tick();

        verify(picker, times(1)).pickNext(2);
        verifyNoInteractions(client, writer);
    }

    @Test
    void blockedResultIsSavedStopsTheTickAndPauses() {
        MetFetchResult blocked = MetFetchResult.blocked("blocked (403)");
        when(picker.pickNext(2)).thenReturn(List.of(a, b));
        when(client.fetch("100")).thenReturn(blocked);

        worker.tick();
        worker.tick();

        verify(client, times(1)).fetch("100");   // no quick retry for a block
        verify(writer).save(1L, blocked);         // the reason is still stored
        verify(client, never()).fetch("200");     // the tick stopped before b
        verify(picker, times(1)).pickNext(2);     // the second tick was paused
    }

    @Test
    void failureIsRetriedOnceAndOnlyTheRetryResultIsSaved() {
        MetFetchResult failed = MetFetchResult.failed("network: timeout");
        MetFetchResult rejected = MetFetchResult.rejected("not public domain");
        when(picker.pickNext(2)).thenReturn(List.of(a));
        when(client.fetch("100")).thenReturn(failed, rejected);   // 1st call, 2nd call

        worker.tick();

        verify(client, times(2)).fetch("100");
        verify(writer).save(1L, rejected);
        verify(writer, never()).save(1L, failed);
    }
}
