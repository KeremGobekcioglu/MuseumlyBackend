package com.kg.museumly.backend;

import com.kg.museumly.backend.exception.ApiException;
import com.kg.museumly.backend.security.GoogleTokenVerificationClient;
import com.kg.museumly.backend.security.GoogleTokenVerifier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoogleTokenVerifierTest {

    @Test
    void verify_throwsApiException_whenTokenIsInvalid() throws Exception
    {
        GoogleTokenVerificationClient client = mock(GoogleTokenVerificationClient.class);
        when(client.verify("bad-token")).thenReturn(null);

        GoogleTokenVerifier googleTokenVerifier = new GoogleTokenVerifier(client);

        assertThatThrownBy(() -> googleTokenVerifier.verify("bad-token"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Invalid Google ID token");
    }
}
