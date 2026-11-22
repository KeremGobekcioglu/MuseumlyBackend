package com.kg.museumly.backend.repository;

import com.kg.museumly.backend.entity.Artwork;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ArtworkRepository extends Repository<Artwork, Long> {

    /**
     * One page of ACCEPTED Met artworks in a department, ordered by id.
     * Keyset paging: returns rows with id > afterId (first page: afterId = 0).
     * Years are optional: both null = no year filter (feed); otherwise eraYear
     * must be in [fromYear, toYear], both ends included (era page).
     * Rows with a NULL eraYear show up in the feed, never on an era page.
     */
    @Query("""
        SELECT a FROM Artwork a
        WHERE a.source = 'met'
        AND a.status = 'ACCEPTED'
        AND a.department = :department
        AND a.id > :afterId
        AND (:fromYear IS NULL OR a.eraYear BETWEEN :fromYear and :toYear)
        ORDER by a.id
""")
    public List<Artwork> findAcceptedPage(
            @Param("department") String department,
            @Param("afterId") long id,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear,
            Limit limit
    );
    /**
     * Ids of rows in the same slice (department + years, id > afterId) that are
     * still waiting for the crawler (DISCOVERED or FAILED).
     * No ORDER BY: only "is there any?" matters. Use existsWaitingAfter instead.
     */
    @Query("""
        SELECT a.id FROM Artwork a
        WHERE a.source = 'met'
          AND a.status IN ('DISCOVERED', 'FAILED')
          AND a.department = :department
          AND a.id > :afterId
          AND (:fromYear IS NULL OR a.eraYear BETWEEN :fromYear AND :toYear)
        """)
    List<Long> findWaitingIds(
            @Param("department") String department,
            @Param("afterId") long afterId,
            @Param("fromYear") Integer fromYear,
            @Param("toYear") Integer toYear,
            Limit limit);
    /**
     * True if at least one row after afterId in this slice is still waiting
     * for the crawler, so it may become ACCEPTED later.
     * Fetches at most one id (JPQL can't select EXISTS directly).
     */
    default boolean existsWaitingAfter(String department, long afterId, Integer fromYear, Integer toYear) {
        List<Long> ids = findWaitingIds(department, afterId, fromYear, toYear, Limit.of(1));
        return !ids.isEmpty();
    }
}
