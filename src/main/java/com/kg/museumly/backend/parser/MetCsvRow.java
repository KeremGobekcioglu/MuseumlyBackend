package com.kg.museumly.backend.parser;

public record MetCsvRow(
        String sourceId,
        String department,
        String title,
        String objectName,
        String artistDisplayName,
        String artistDisplayBio,
        String objectDate,
        Integer beginYear,
        Integer endYear,
        String classification,
        String medium,
        String dimensions,
        String creditLine,
        String culture,
        String period
) {
}