package com.kg.museumly.backend;

import com.kg.museumly.backend.crawler.MetCandidatePicker;
import com.kg.museumly.backend.crawler.MetEnrichmentWorker;
import com.kg.museumly.backend.crawler.MetEnrichmentWriter;
import com.kg.museumly.backend.crawler.MetFetchResult;
import com.kg.museumly.backend.crawler.MetObjectClassifier;
import com.kg.museumly.backend.crawler.MetObjectClient;
import com.kg.museumly.backend.crawler.MetObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// Tests: MetEnrichmentWorker.tick() with the real picker + writer + Postgres; only the Met HTTP client is mocked.
// Extend: stub client.fetch(sourceId) with a MetFetchResult, call worker.tick(), then assert on the artworks row.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MetWorkerPipelineIntegrationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Autowired
    private MetCandidatePicker picker;

    @Autowired
    private MetEnrichmentWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MetObjectClient client;
    private MetEnrichmentWorker worker;

    @BeforeEach
    void setUp() {
        client = mock(MetObjectClient.class);
        worker = new MetEnrichmentWorker(1, 0L, picker, client, writer);   // target 1 per department, no pacing
    }

    @Test
    void tick_savesEachOutcomeToTheDatabase() {
        String accepted = insert("European Paintings");
        String rejected = insert("Egyptian Art");
        String failed = insert("Asian Art");
        when(client.fetch(accepted)).thenReturn(MetObjectClassifier.classify(200, read("/met/object-436535.json")));
        when(client.fetch(rejected)).thenReturn(MetObjectClassifier.classify(404, null));
        when(client.fetch(failed)).thenReturn(MetFetchResult.failed("network: timeout"));

        worker.tick();

        assertThat(row(accepted).get("status")).isEqualTo("ACCEPTED");
        assertThat(row(accepted).get("image_small_url")).isNotNull();
        assertThat(row(rejected).get("status")).isEqualTo("REJECTED");
        assertThat(row(failed).get("status")).isEqualTo("FAILED");
        assertThat(row(failed).get("attempts")).isEqualTo(1);
        verify(client, times(2)).fetch(failed);   // transient failure is retried once before saving
    }

    @Test
    void tick_stopsFetchingADepartmentOnceItReachedTheTarget() {
        String first = insert("Islamic Art");
        String second = insert("Islamic Art");
        when(client.fetch(first)).thenReturn(MetObjectClassifier.classify(200, read("/met/object-436535.json")));

        worker.tick();   // accepts `first`, department is now at target 1
        worker.tick();   // nothing left to pick

        assertThat(row(first).get("status")).isEqualTo("ACCEPTED");
        assertThat(row(second).get("status")).isEqualTo("DISCOVERED");
        verify(client, never()).fetch(second);
    }

    @Test
    void tick_onBlockedResponse_storesTheFailureAndStopsBeforeTheNextArtwork() {
        String first = insert("Asian Art");
        String second = insert("Egyptian Art");
        when(client.fetch(first)).thenReturn(MetFetchResult.blocked("blocked (429)"));
        when(client.fetch(second)).thenReturn(MetFetchResult.blocked("blocked (429)"));

        worker.tick();

        // departments are picked in alphabetical order, so Asian Art goes first
        assertThat(row(first).get("status")).isEqualTo("FAILED");
        assertThat(row(first).get("last_error")).isEqualTo("blocked (429)");
        assertThat(row(second).get("status")).isEqualTo("DISCOVERED");
        verify(client, never()).fetch(second);
    }

    // inserts a DISCOVERED met row and returns its source_id
    private String insert(String department) {
        String sourceId = UUID.randomUUID().toString();
        jdbcTemplate.update("INSERT INTO artworks (source, source_id, department) VALUES ('met', ?, ?)",
                sourceId, department);
        return sourceId;
    }

    private Map<String, Object> row(String sourceId) {
        return jdbcTemplate.queryForMap(
                "SELECT status, attempts, last_error, image_small_url FROM artworks "
                        + "WHERE source = 'met' AND source_id = ?", sourceId);
    }

    private MetObjectResponse read(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        return mapper.readValue(stream, MetObjectResponse.class);
    }
}
