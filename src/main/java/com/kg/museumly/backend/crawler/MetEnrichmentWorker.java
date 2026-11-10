package com.kg.museumly.backend.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
// match exactly for havingValue. use lowercase true.
@ConditionalOnProperty(name = "museumly.crawler.met.enabled", havingValue = "true")
public class MetEnrichmentWorker {

    private static final Logger log = LoggerFactory.getLogger(MetEnrichmentWorker.class);
    private final int targetPerDepartment;
    private final long pacingMs;

    public MetEnrichmentWorker(
        @Value("${museumly.crawler.met.target-per-department}") int targetPerDepartment,
        @Value("${museumly.crawler.met.pacing-ms}") long pacingMs
    )
    {
        this.targetPerDepartment = targetPerDepartment;
        this.pacingMs = pacingMs;
    }

    @Scheduled(fixedDelay = 1000)
    public void tick()
    {
        log.info("tick (target={}, pacing={}ms)", targetPerDepartment, pacingMs);
    }
}
