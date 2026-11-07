package com.kg.museumly.backend.crawler;

public record MetFetchResult(
        MetOutcome outcome,
        boolean blocked,
        String error,
        MetObjectResponse body
) {

    public static MetFetchResult accepted(MetObjectResponse body) {
        return new MetFetchResult(MetOutcome.ACCEPTED, false, null, body);
    }

    public static MetFetchResult rejected(String error) {
        return new MetFetchResult(MetOutcome.REJECTED, false, error, null);
    }

    public static MetFetchResult failed(String error) {
        return new MetFetchResult(MetOutcome.FAILED, false, error, null);
    }

    public static MetFetchResult blocked(String error) {
        return new MetFetchResult(MetOutcome.FAILED, true, error, null);
    }
}