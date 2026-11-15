package com.kg.museumly.backend.dtos.artwork;

import java.util.List;

public record MetArtworkPage(
        List<MetArtworkItem> items,
        String nextCursor,
        boolean exhausted
) {}
