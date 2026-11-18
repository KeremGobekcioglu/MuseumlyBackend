package com.kg.museumly.backend.repository;

import com.kg.museumly.backend.entity.Artwork;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ArtworkRepository extends Repository<Artwork, Long> {

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

    default boolean existsWaitingAfter(String department, long afterId, Integer fromYear, Integer toYear) {
        List<Long> ids = findWaitingIds(department, afterId, fromYear, toYear, Limit.of(1));
        return !ids.isEmpty();
    }
}
