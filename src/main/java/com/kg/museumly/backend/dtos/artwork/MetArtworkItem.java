package com.kg.museumly.backend.dtos.artwork;

import java.util.List;

public record MetArtworkItem(
        String sourceId,
        String title,
        String artist,
        String artistBio,
        String objectDate,
        Integer beginYear,
        String classification,
        String department,
        String medium,
        String dimensions,
        String creditLine,
        String culture,
        String period,
        String imageSmallUrl,
        String imageUrl,
        Integer imageWidth,
        Integer imageHeight
) {}

