package com.kg.museumly.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kg.museumly.backend.exception.ApiException;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
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

    @Test
    void verify_throwsApiException_whenVerificationFails() throws Exception {
        GoogleTokenVerificationClient client = mock(GoogleTokenVerificationClient.class);
        when(client.verify("any-token")).thenThrow(new GeneralSecurityException("boom"));

        GoogleTokenVerifier tokenVerifier = new GoogleTokenVerifier(client);

        assertThatThrownBy(() -> tokenVerifier.verify("any-token"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Could not verify Google ID token");
    }

    @Test
    void verify_returnsPayload_whenTokenIsValid() throws Exception {
        GoogleTokenVerificationClient client = mock(GoogleTokenVerificationClient.class);
        GoogleIdToken mockToken = mock(GoogleIdToken.class);
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setSubject("some-google-sub");

        when(client.verify("good-token")).thenReturn(mockToken);
        when(mockToken.getPayload()).thenReturn(payload);

        GoogleTokenVerifier tokenVerifier = new GoogleTokenVerifier(client);

        assertThat(tokenVerifier.verify("good-token").getSubject()).isEqualTo("some-google-sub");
    }
}
