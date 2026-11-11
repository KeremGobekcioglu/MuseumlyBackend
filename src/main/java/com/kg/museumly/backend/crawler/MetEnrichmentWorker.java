package com.kg.museumly.backend.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
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

    private static final Duration IDLE_PAUSE = Duration.ofMinutes(1);
    private static final Duration FIRST_COOLDOWN = Duration.ofMinutes(1);
    private static final Duration MAX_COOLDOWN = Duration.ofMinutes(30);

    private Instant pausedUntil = Instant.EPOCH;   // 1970 = not paused
    private Duration currentCooldown = null;       // null = not in a block streak

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
    public void tick()
    {
        // check if we have active wait time
        if(Instant.now().isBefore(pausedUntil))
        {
            return;
        }
        // select candidates
        List<MetCandidate> candidates= picker.pickNext(targetPerDepartment);
        // empty means we need to wait in idle state
        if(candidates.isEmpty())
        {
            log.info("idle");
            pausedUntil = Instant.now().plus(IDLE_PAUSE);
            return;
        }
        // enrich every candidate
        for(MetCandidate candidate: candidates)
        {
            // get images for each met artwork.
            MetFetchResult result = client.fetch(candidate.sourceId());
            //İF RESULT is not blocked , wait 1.5 seconds and try again
            if(result.outcome() == MetOutcome.FAILED && !result.blocked())
            {
                log.info("met {} ({}) → FAILED: {}; retrying",
                        candidate.sourceId(), candidate.department(), result.error());
                if(!sleepPacing())
                {
                    return;
                }
                result = client.fetch(candidate.sourceId());
            }
            writer.save(candidate.id(), result);
            if (result.error() == null) {
                log.info("met {} ({}) → {}", candidate.sourceId(), candidate.department(), result.outcome());
            } else {
                log.info("met {} ({}) → {}: {}", candidate.sourceId(), candidate.department(), result.outcome(), result.error());
            }
            // 403, 429 ... we need to wait.
            if(result.blocked())
            {
                startCooldown(result);
                return;
            }
            // reset cooldown, everything is good.
            currentCooldown = null;
            // sleep 1.5ms between calls.
            if(!sleepPacing())
            {
                return;
            }
        }
    }

    private void startCooldown(MetFetchResult result)
    {
        // if this function is called first time, cooldown should be one minute
        if(currentCooldown == null)
        {
            currentCooldown = FIRST_COOLDOWN;
        }
        // double
        else
        {
            Duration doubled = currentCooldown.multipliedBy(2);
            // check if it surpasses 30, which is max.
            if(doubled.compareTo(MAX_COOLDOWN) > 0)
            {
                currentCooldown = MAX_COOLDOWN;
            }
            else
            {
                currentCooldown = doubled;
            }
        }
        // pause until cooldown ends.
        pausedUntil = Instant.now().plus(currentCooldown);
        log.warn("met blocked ({}); pausing {} min until {}", result.error(), currentCooldown.toMinutes(), pausedUntil);
    }

    private boolean sleepPacing()
    {
        try {
            Thread.sleep(pacingMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
