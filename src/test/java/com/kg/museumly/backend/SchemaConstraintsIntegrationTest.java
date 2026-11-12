package com.kg.museumly.backend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Tests: Flyway migrations applied on real Postgres and the DB-level constraints on artworks/users.
// Extend: after adding a migration bump the expected count; for a new constraint add one insert + assertThatThrownBy.
// A failing statement poisons the Postgres transaction, so keep it as the LAST statement of its test.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class SchemaConstraintsIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void allFlywayMigrationsAppliedSuccessfully() {
        Integer applied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE success", Integer.class);
        Integer failed = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE NOT success", Integer.class);

        assertThat(applied).isEqualTo(4);   // V1..V4
        assertThat(failed).isZero();
    }

    @Test
    void artworks_rejectsUnknownStatus() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO artworks (source, source_id, department, status) VALUES ('met', 's1', 'Asian Art', 'BOGUS')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void artworks_sourceAndSourceIdMustBeUnique_butSameIdInAnotherSourceIsFine() {
        jdbcTemplate.update("INSERT INTO artworks (source, source_id, department) VALUES ('met', 'dup', 'Asian Art')");
        jdbcTemplate.update("INSERT INTO artworks (source, source_id, department) VALUES ('cleveland', 'dup', 'Asian Art')");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO artworks (source, source_id, department) VALUES ('met', 'dup', 'Asian Art')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void artworks_newRowDefaultsToDiscoveredWithZeroAttempts() {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO artworks (source, source_id, department) VALUES ('met', 'defaults', 'Asian Art') RETURNING id",
                Long.class);

        var row = jdbcTemplate.queryForMap("SELECT status, attempts FROM artworks WHERE id = ?", id);

        assertThat(row.get("status")).isEqualTo("DISCOVERED");
        assertThat(row.get("attempts")).isEqualTo(0);
    }

    @Test
    void users_googleSubMustBeUnique() {
        jdbcTemplate.update("INSERT INTO users (google_sub, email) VALUES ('sub-x', 'a@example.com')");

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO users (google_sub, email) VALUES ('sub-x', 'b@example.com')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
