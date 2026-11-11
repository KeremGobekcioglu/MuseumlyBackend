package com.kg.museumly.backend.crawler;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class MetCandidatePicker {

    // that '?' is target per department. so if it s number N, query will return until it reaches N.
    private static final String PICK_SQL = """
            SELECT DISTINCT ON (a.department)
                   a.id, a.source_id, a.department
            FROM artworks a
            WHERE a.source = 'met'
              AND (a.status = 'DISCOVERED'
                   OR (a.status = 'FAILED' AND a.next_attempt_at <= now()))
              AND a.department IN (
                    SELECT d.department
                    FROM artworks d
                    WHERE d.source = 'met'
                    GROUP BY d.department
                    HAVING COUNT(*) FILTER (WHERE d.status = 'ACCEPTED') < ?
              )
            ORDER BY a.department, a.id
            """;
    private final JdbcTemplate jdbcTemplate;

    public MetCandidatePicker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<MetCandidate> pickNext(int targetPerDepartment)
    {
        // that '?' is target per department.
        return jdbcTemplate.query(PICK_SQL, this::mapRow, targetPerDepartment);
    }

    private MetCandidate mapRow(ResultSet rs, int rowNum) throws SQLException
    {
        long id = rs.getLong("id");
        String sourceId = rs.getString("source_id");
        String department = rs.getString("department");
        return new MetCandidate(id, sourceId, department);
    }
}
