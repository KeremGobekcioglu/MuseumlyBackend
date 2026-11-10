package com.kg.museumly.backend.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
// match exactly for havingValue. use lowercase true.
@ConditionalOnProperty(name = "museumly.crawler.met.enabled", havingValue = "true")
public class MetEnrichmentWorker {

    private static final Logger log = LoggerFactory.getLogger(MetEnrichmentWorker.class);
    private final int targetPerDepartment;
    private final long pacingMs;
    private final MetCandidatePicker picker;
    private final MetObjectClient client;
    private final MetEnrichmentWriter writer;
    public MetEnrichmentWorker(
            @Value("${museumly.crawler.met.target-per-department}") int targetPerDepartment,
            @Value("${museumly.crawler.met.pacing-ms}") long pacingMs, MetCandidatePicker picker, MetObjectClient client, MetEnrichmentWriter writer
    )
    {
        this.targetPerDepartment = targetPerDepartment;
        this.pacingMs = pacingMs;
        this.picker = picker;
        this.client = client;
        this.writer = writer;
    }

    @Scheduled(fixedDelay = 1000)
    public void tick() throws InterruptedException
    {
        // select candidates
        List<MetCandidate> candidates= picker.pickNext(targetPerDepartment);
        // enrich every candidate
        for(MetCandidate candidate: candidates)
        {
            // get images for each met artwork.
            MetFetchResult result = client.fetch(candidate.sourceId());
            writer.save(candidate.id(), result);
            if (result.error() == null) {
                log.info("met {} ({}) → {}", candidate.sourceId(), candidate.department(), result.outcome());
            } else {
                log.info("met {} ({}) → {}: {}", candidate.sourceId(), candidate.department(), result.outcome(), result.error());
            }
            // sleep 1.5ms between calls.
            Thread.sleep(pacingMs);
        }
    }
}
