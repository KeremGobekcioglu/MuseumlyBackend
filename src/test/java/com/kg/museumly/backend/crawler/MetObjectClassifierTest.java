package com.kg.museumly.backend.crawler;

import org.junit.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
public class MetObjectClassifierTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    public void realVanGogh_isAccepted_withImagesAndExtras() {
        MetObjectResponse body = read("/met/object-436535.json");

        MetFetchResult result = MetObjectClassifier.classify(200, body);

        assertThat(result.outcome()).isEqualTo(MetOutcome.ACCEPTED);
        assertThat(result.body().primaryImageSmall())
                .isEqualTo("https://images.metmuseum.org/CRDImages/ep/web-large/DP-42549-001.jpg");
        assertThat(result.body().additionalImages().size()).isEqualTo(4);
        assertThat(result.body().constituents().isArray()).isTrue();
        assertThat(result.body().measurements().isArray()).isTrue();
    }

    @Test
    public void realCoin_isRejected_asNotPublicDomain() {
        MetObjectResponse body = read("/met/object-1.json");

        MetFetchResult result = MetObjectClassifier.classify(200, body);

        assertThat(result.outcome()).isEqualTo(MetOutcome.REJECTED);
        assertThat(result.error()).isEqualTo("not public domain");
    }

    @Test
    public void publicDomainWithoutSmallImage_isRejected() {
        MetObjectResponse body = new MetObjectResponse(
                true, "https://images.metmuseum.org/x.jpg", "", null, null, null);

        MetFetchResult result = MetObjectClassifier.classify(200, body);

        assertThat(result.outcome()).isEqualTo(MetOutcome.REJECTED);
        assertThat(result.error()).isEqualTo("no image");
    }

    @Test
    public void okStatusWithUnexpectedBody_isFailed_neverRejected() {
        MetObjectResponse body = mapper.readValue(
                "{\"message\":\"ObjectID not found\"}", MetObjectResponse.class);

        MetFetchResult result = MetObjectClassifier.classify(200, body);

        assertThat(result.outcome()).isEqualTo(MetOutcome.FAILED);
        assertThat(result.blocked()).isFalse();
    }

    @Test
    public void notFound_isRejected_whileBlocksAndServerErrors_areFailed() {
        MetFetchResult notFound = MetObjectClassifier.classify(404, null);
        MetFetchResult forbidden = MetObjectClassifier.classify(403, null);
        MetFetchResult tooMany = MetObjectClassifier.classify(429, null);
        MetFetchResult serverError = MetObjectClassifier.classify(503, null);

        assertThat(notFound.outcome()).isEqualTo(MetOutcome.REJECTED);

        assertThat(forbidden.outcome()).isEqualTo(MetOutcome.FAILED);
        assertThat(forbidden.blocked()).isTrue();

        assertThat(tooMany.outcome()).isEqualTo(MetOutcome.FAILED);
        assertThat(tooMany.blocked()).isTrue();

        assertThat(serverError.outcome()).isEqualTo(MetOutcome.FAILED);
        assertThat(serverError.blocked()).isFalse();
    }

    private MetObjectResponse read(String path) {
        InputStream stream = getClass().getResourceAsStream(path);
        return mapper.readValue(stream, MetObjectResponse.class);
    }
}
