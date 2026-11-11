package com.kg.museumly.backend.crawler;

/**
 * id for the UPDATE, sourceId for /objects/{sourceId}, department for logs.
 * @param id
 * @param sourceId
 * @param department
 */
public record MetCandidate(long id, String sourceId, String department) {
}
