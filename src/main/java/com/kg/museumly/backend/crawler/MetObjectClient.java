package com.kg.museumly.backend.crawler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;


@Component
public class MetObjectClient {

    private static final Logger logger = LoggerFactory.getLogger(MetObjectClient.class);
    private static final String BASE_URL = "https://collectionapi.metmuseum.org/public/collection";
    private final RestClient restClient;

    public MetObjectClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = builder
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public MetFetchResult fetch(String sourceId)
    {
        try {
            return restClient.get()
                    .uri("/v1/objects/{id}", sourceId)
                    // retrieve would throw exception for every 4xx/5xx
                    .exchange(
                            (request,response) ->
                            {
                                int status = response.getStatusCode().value();
                                if (status != 200)
                                {
                                    return MetObjectClassifier.classify(status,null);
                                }
                                MetObjectResponse body = response.bodyTo(MetObjectResponse.class);
                                return MetObjectClassifier.classify(status,body);
                            }
                    );
        }
        catch (ResourceAccessException e)
        {
            return MetFetchResult.failed("network: " + e.getMessage());
        }
        catch (RestClientException e)
        {
            return MetFetchResult.failed("bad response: " + e.getMessage());
        }
        catch (RuntimeException e)
        {
            logger.warn("Unexpected error fetching Met object {}", sourceId, e);
            return MetFetchResult.failed("unexpected: " + e.getMessage());
        }
    }

}
