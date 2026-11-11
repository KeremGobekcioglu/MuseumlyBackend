package com.kg.museumly.backend.crawler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetObjectResponse(
        @JsonProperty("isPublicDomain") Boolean isPublicDomain,
        String primaryImage,
        String primaryImageSmall,
        JsonNode additionalImages,
        JsonNode constituents,
        JsonNode measurements
) {
}
