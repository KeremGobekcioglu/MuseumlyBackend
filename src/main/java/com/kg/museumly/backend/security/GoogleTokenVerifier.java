package com.kg.museumly.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kg.museumly.backend.exception.ApiException;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleTokenVerifier {

    private final GoogleTokenVerificationClient client;

    public GoogleTokenVerifier(GoogleTokenVerificationClient client) {
        this.client = client;
    }

    public GoogleIdToken.Payload verify(String idTokenString)
    {
        GoogleIdToken idToken;
        try {
            idToken = client.verify(idTokenString);
        }
        catch (GeneralSecurityException | IOException e)
        {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN",
                    "Could not verify Google ID token");
        }

        if(idToken == null)
        {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_GOOGLE_TOKEN",
                    "Invalid Google ID token");
        }
        return idToken.getPayload();
    }
}
