package com.kg.museumly.backend;

import com.kg.museumly.backend.crawler.MetCandidate;
import com.kg.museumly.backend.crawler.MetCandidatePicker;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class MetCandidatePickerTest {

    @Autowired
    private MetCandidatePicker picker;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void departmentAtTarget_dropsOut_othersGetTheirLowestCandidate() {
        insert("European Paintings", "ACCEPTED");
        insert("European Paintings", "ACCEPTED");
        insert("European Paintings", "DISCOVERED");

        insert("Islamic Art", "REJECTED");
        long firstIslamic = insert("Islamic Art", "DISCOVERED");
        insert("Islamic Art", "DISCOVERED");

        List<MetCandidate> picked = picker.pickNext(2);

        assertThat(picked).hasSize(1);
        assertThat(picked.get(0).id()).isEqualTo(firstIslamic);
    }

    @Test
    void failedRow_isPickedOnlyAfterNextAttemptAt() {
        insertFailed("Egyptian Art", 60);
        long dueAsian = insertFailed("Asian Art", -1);

        List<MetCandidate> picked = picker.pickNext(100);

        assertThat(picked).hasSize(1);
        assertThat(picked.get(0).id()).isEqualTo(dueAsian);
    }

    private long insert(String department, String status) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO artworks (source, source_id, department, status)
                VALUES ('met', ?, ?, ?)
                RETURNING id
                """, Long.class, UUID.randomUUID().toString(), department, status);
    }

    private long insertFailed(String department, int minutesFromNow) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO artworks (source, source_id, department, status, next_attempt_at)
                VALUES ('met', ?, ?, 'FAILED', now() + make_interval(mins => ?))
                RETURNING id
                """, Long.class, UUID.randomUUID().toString(), department, minutesFromNow);
    }
}