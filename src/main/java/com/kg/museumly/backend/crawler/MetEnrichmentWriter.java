package com.kg.museumly.backend.crawler;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class MetEnrichmentWriter {

    private final JdbcTemplate jdbcTemplate;

    public MetEnrichmentWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String ACCEPT_SQL = """
            UPDATE artworks
            SET status            = 'ACCEPTED',
                image_url         = ?,
                image_small_url   = ?,
                additional_images = CAST(? AS jsonb),
                constituents      = CAST(? AS jsonb),
                measurements      = CAST(? AS jsonb),
                last_error        = NULL,
                next_attempt_at   = NULL,
                updated_at        = now()
            WHERE id = ?
            """;

    private static final String REJECT_SQL = """
            UPDATE artworks
            SET status          = 'REJECTED',
                last_error      = ?,
                next_attempt_at = NULL,
                updated_at      = now()
            WHERE id = ?
            """;

    private static final String FAIL_SQL = """
            UPDATE artworks
            SET status          = 'FAILED',
                attempts        = attempts + 1,
                last_error      = ?,
                next_attempt_at = now() + interval '3 minutes',
                updated_at      = now()
            WHERE id = ?
            """;
    public void save(long id, MetFetchResult result)
    {
        switch (result.outcome())
        {
            case ACCEPTED -> accept(id, result.body());
            case FAILED -> jdbcTemplate.update(FAIL_SQL, result.error(), id);
            case REJECTED -> jdbcTemplate.update(REJECT_SQL,result.error(),id);
        }
    }
    private void accept(long id, MetObjectResponse body)
    {
        jdbcTemplate.update
                (
                        ACCEPT_SQL,
                        blankToNull(body.primaryImage()),
                        body.primaryImageSmall(),
                        toJson(body.additionalImages()),
                        toJson(body.constituents()),
                        toJson(body.measurements()),
                        id
                );
    }
    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String toJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return node.toString();
    }
}
