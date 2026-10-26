package com.kg.museumly.backend.parser;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;

@Component
public class MetArtworkWriter {

    private static final String UPSERT_SQL = """
            INSERT INTO artworks (
                source, source_id, department, title, object_name,
                artist_display_name, artist_display_bio, object_date,
                begin_year, end_year, classification, medium, dimensions,
                credit_line, culture, period
            ) VALUES ('met', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (source, source_id) DO UPDATE SET
                department          = EXCLUDED.department,
                title               = EXCLUDED.title,
                object_name         = EXCLUDED.object_name,
                artist_display_name = EXCLUDED.artist_display_name,
                artist_display_bio  = EXCLUDED.artist_display_bio,
                object_date         = EXCLUDED.object_date,
                begin_year          = EXCLUDED.begin_year,
                end_year            = EXCLUDED.end_year,
                classification      = EXCLUDED.classification,
                medium              = EXCLUDED.medium,
                dimensions          = EXCLUDED.dimensions,
                credit_line         = EXCLUDED.credit_line,
                culture             = EXCLUDED.culture,
                period              = EXCLUDED.period,
                updated_at          = now()
            """;

    private final JdbcTemplate jdbcTemplate;

    public MetArtworkWriter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void upsertBatch(List<MetCsvRow> rows)
    {
        jdbcTemplate.batchUpdate(UPSERT_SQL, rows, rows.size(),
                (PreparedStatement ps, MetCsvRow row) -> {
                    ps.setString(1, row.sourceId());
                    ps.setString(2, row.department());
                    ps.setString(3, row.title());
                    ps.setString(4, row.objectName());
                    ps.setString(5, row.artistDisplayName());
                    ps.setString(6, row.artistDisplayBio());
                    ps.setString(7, row.objectDate());
                    ps.setObject(8, row.beginYear(), Types.INTEGER);
                    ps.setObject(9, row.endYear(), Types.INTEGER);
                    ps.setString(10, row.classification());
                    ps.setString(11, row.medium());
                    ps.setString(12, row.dimensions());
                    ps.setString(13, row.creditLine());
                    ps.setString(14, row.culture());
                    ps.setString(15, row.period());
                });
    }
}
