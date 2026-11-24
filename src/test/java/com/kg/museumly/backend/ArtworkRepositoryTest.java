package com.kg.museumly.backend;

import com.kg.museumly.backend.entity.Artwork;
import com.kg.museumly.backend.met.MetDepartment;
import com.kg.museumly.backend.repository.ArtworkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.kg.museumly.backend.repository.ArtworkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
public class ArtworkRepositoryTest {

    @Autowired
    private ArtworkRepository repository;

    @Autowired
    private JdbcTemplate jdbc;
    private long insert(String department, String status, Integer beginYear, Integer endYear) {
        return jdbc.queryForObject("""
            INSERT INTO artworks (source, source_id, department, status, begin_year, end_year)
            VALUES ('met', ?, ?, ?, ?, ?)
            RETURNING id
            """, Long.class, UUID.randomUUID().toString(), department, status, beginYear, endYear);
    }
    private static final String PAINTINGS = MetDepartment.EUROPEAN_PAINTINGS.getCsvName();
    private static final String ASIAN = MetDepartment.ASIAN_ART.getCsvName();

    @BeforeEach
    void cleanTable()
    {
        jdbc.update("DELETE FROM artworks");
    }

    private List<Long> ids(List<Artwork> artworks) {
        List<Long> result = new ArrayList<>();
        for (Artwork artwork : artworks) {
            result.add(artwork.getId());
        }
        return result;
    }

    @Test
    public void returnsOnlyAcceptedRowsOfTheDepartment()
    {
        long wanted = insert(PAINTINGS, "ACCEPTED" , 1600, 1650);
        insert(PAINTINGS, "REJECTED", 1600, 1650);
        insert(ASIAN, "ACCEPTED", 1600, 1650);
        List<Artwork> artworks = repository.findAcceptedPage(PAINTINGS, 0, null, null, Limit.of(20));

        assertEquals(List.of(wanted), ids(artworks));
    }

    @Test
    void nextPageStartsAfterTheCursorWithoutOverlap() {
        long first = insert(PAINTINGS, "ACCEPTED", 1600, 1650);
        long second = insert(PAINTINGS, "ACCEPTED", 1600, 1650);
        long third = insert(PAINTINGS, "ACCEPTED", 1600, 1650);
        List<Artwork> page1 = repository.findAcceptedPage(PAINTINGS, 0, null, null, Limit.of(2));
        long cursor = page1.get(page1.size() - 1).getId();
        List<Artwork> page2 = repository.findAcceptedPage(PAINTINGS, cursor, null, null, Limit.of(2));
        assertEquals(List.of(first, second), ids(page1));
        assertEquals(List.of(third), ids(page2));
    }

    @Test
    void eraRangeIncludesBothEndsAndSkipsRowsWithoutYears() {
        insert(PAINTINGS, "ACCEPTED", 1599, 1599);
        long start = insert(PAINTINGS, "ACCEPTED", 1600, 1600);
        long end = insert(PAINTINGS, "ACCEPTED", 1699, 1699);
        insert(PAINTINGS, "ACCEPTED", 1700, 1700);
        insert(PAINTINGS, "ACCEPTED", null, null);

        List<Artwork> era = repository.findAcceptedPage(PAINTINGS, 0, 1600, 1699, Limit.of(20));

        assertEquals(List.of(start, end), ids(era));
    }

    @Test
    void feedWithoutYearsIncludesEveryRowEvenWithoutYears() {
        long a = insert(PAINTINGS, "ACCEPTED", 1599, 1599);
        long b = insert(PAINTINGS, "ACCEPTED", 1700, 1700);
        long noYears = insert(PAINTINGS, "ACCEPTED", null, null);

        List<Artwork> feed = repository.findAcceptedPage(PAINTINGS, 0, null, null, Limit.of(20));

        assertEquals(List.of(a, b, noYears), ids(feed));
    }

    @Test
    void workSpanningYearZeroBelongsToTheEraBeforeIt() {
        long spanning = insert(PAINTINGS, "ACCEPTED", -100, 99);

        List<Artwork> bce = repository.findAcceptedPage(PAINTINGS, 0, -500, -1, Limit.of(20));
        List<Artwork> ce = repository.findAcceptedPage(PAINTINGS, 0, 0, 499, Limit.of(20));

        assertEquals(List.of(spanning), ids(bce));
        assertEquals(List.of(), ids(ce));
    }
    @Test
    void waitingRowAfterTheCursorCountsButBeforeItDoesNot() {
        long waiting = insert(PAINTINGS, "DISCOVERED", 1600, 1650);
        long accepted = insert(PAINTINGS, "ACCEPTED", 1600, 1650);

        assertTrue(repository.existsWaitingAfter(PAINTINGS, 0, null, null));
        assertFalse(repository.existsWaitingAfter(PAINTINGS, accepted, null, null));
    }

    @Test
    void failedRowsCountAsWaiting() {
        insert(PAINTINGS, "FAILED", 1600, 1650);

        assertTrue(repository.existsWaitingAfter(PAINTINGS, 0, null, null));
    }

    @Test
    void acceptedAndRejectedRowsAreNotWaiting() {
        insert(PAINTINGS, "ACCEPTED", 1600, 1650);
        insert(PAINTINGS, "REJECTED", 1600, 1650);

        assertFalse(repository.existsWaitingAfter(PAINTINGS, 0, null, null));
    }

    @Test
    void waitingCheckStaysInsideTheSlice() {
        insert(PAINTINGS, "DISCOVERED", 1650, 1650);
        insert(ASIAN, "DISCOVERED", 1750, 1750);

        assertTrue(repository.existsWaitingAfter(PAINTINGS, 0, 1600, 1699));
        assertFalse(repository.existsWaitingAfter(PAINTINGS, 0, 1700, 1799));
    }
}
