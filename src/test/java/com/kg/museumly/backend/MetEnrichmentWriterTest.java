package com.kg.museumly.backend;

import com.kg.museumly.backend.crawler.MetEnrichmentWriter;
import com.kg.museumly.backend.crawler.MetFetchResult;
import com.kg.museumly.backend.crawler.MetObjectClassifier;
import com.kg.museumly.backend.crawler.MetObjectResponse;
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

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MetEnrichmentWriterTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Autowired
    private MetEnrichmentWriter writer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void acceptedRow_storesImagesAndApiExtras() {
        long id = insertDiscovered();
        MetObjectResponse body = read("/met/object-436535.json");

        writer.save(id, MetObjectClassifier.classify(200, body));

        Map<String, Object> row = readRow(id);
        assertThat(row.get("status")).isEqualTo("ACCEPTED");
        assertThat(row.get("image_url"))
                .isEqualTo("https://images.metmuseum.org/CRDImages/ep/original/DP-42549-001.jpg");
        assertThat(row.get("image_small_url"))
                .isEqualTo("https://images.metmuseum.org/CRDImages/ep/web-large/DP-42549-001.jpg");
        assertThat(row.get("extra_images")).isEqualTo(4);
        assertThat(row.get("constituents_type")).isEqualTo("array");
        assertThat(row.get("measurements_type")).isEqualTo("array");
        assertThat(row.get("last_error")).isNull();
    }

    @Test
    void failedRow_getsRetryTime_andLaterAcceptClearsIt() {
        long id = insertDiscovered();

        writer.save(id, MetFetchResult.failed("network: timeout"));

        Map<String, Object> failed = readRow(id);
        assertThat(failed.get("status")).isEqualTo("FAILED");
        assertThat(failed.get("attempts")).isEqualTo(1);
        assertThat(failed.get("last_error")).isEqualTo("network: timeout");
        assertThat(failed.get("retry_later")).isEqualTo(true);

        MetObjectResponse body = read("/met/object-436535.json");
        writer.save(id, MetObjectClassifier.classify(200, body));

        Map<String, Object> accepted = readRow(id);
        assertThat(accepted.get("status")).isEqualTo("ACCEPTED");
        assertThat(accepted.get("last_error")).isNull();
        assertThat(accepted.get("next_attempt_at")).isNull();
        assertThat(accepted.get("attempts")).isEqualTo(1);
    }

    @Test
    void emptyAndNullApiFields_areStoredAsSqlNull() {
        long id = insertDiscovered();
        MetObjectResponse body = mapper.readValue("""
                {
                  "isPublicDomain": true,
                  "primaryImage": "",
                  "primaryImageSmall": "https://images.metmuseum.org/small.jpg",
                  "additionalImages": [],
                  "constituents": null
                }
                """, MetObjectResponse.class);

        writer.save(id, MetObjectClassifier.classify(200, body));

        Map<String, Object> row = readRow(id);
        assertThat(row.get("status")).isEqualTo("ACCEPTED");
        assertThat(row.get("image_url")).isNull();
        assertThat(row.get("extra_images")).isEqualTo(0);
        assertThat(row.get("constituents_type")).isNull();
        assertThat(row.get("measurements_type")).isNull();
    }

    private long insertDiscovered() {
        return jdbcTemplate.queryForObject("""
                INSERT INTO artworks (source, source_id, department)
                VALUES ('met', ?, 'European Paintings')
                RETURNING id
                """, Long.class, UUID.randomUUID().toString());
    }

    private Map<String, Object> readRow(long id) {
        return jdbcTemplate.queryForMap("""
                SELECT status,
                       image_url,
                       image_small_url,
                       jsonb_array_length(additional_images) AS extra_images,
                       jsonb_typeof(constituents)            AS constituents_type,
                       jsonb_typeof(measurements)            AS measurements_type,
                       last_error,
                       attempts,
                       next_attempt_at,
                       next_attempt_at > now()               AS retry_later
                FROM artworks
                WHERE id = ?
                """, id);
    }

    private MetObjectResponse read(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        return mapper.readValue(stream, MetObjectResponse.class);
    }
}