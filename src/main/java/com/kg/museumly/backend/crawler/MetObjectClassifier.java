package com.kg.museumly.backend.crawler;

import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * REJECTED: 404, NO PUBLIC DOMAIN, NO IMAGE.
 * FAILED: STATUS != 200, NO BODY, PUBLIC DOMAIN NULL
 * BLOCKED: 403, 429
 */
public final class MetObjectClassifier {

    private MetObjectClassifier(){}

    public static MetFetchResult classify(int status, MetObjectResponse body)
    {
        // not found
        if(status == 404)
        {
            return  MetFetchResult.rejected("not found(404)");
        }
        // blocked
        if(status == 403 || status == 429)
        {
            return MetFetchResult.blocked("blocked (" + status + ")");
        }
        if (status != 200) {
            return MetFetchResult.failed("unexpected status " + status);
        }
        if (body == null) {
            return MetFetchResult.failed("empty body");
        }

        Boolean publicDomain = body.isPublicDomain();
        if(publicDomain == null)
        {
            return MetFetchResult.failed("isPublicDomain missing");
        }
        if(!publicDomain)
        {
            return MetFetchResult.rejected("not public domain");
        }
        if (isBlank(body.primaryImageSmall())) {
            return MetFetchResult.rejected("no image");
        }
        return MetFetchResult.accepted(body);
    }
}
