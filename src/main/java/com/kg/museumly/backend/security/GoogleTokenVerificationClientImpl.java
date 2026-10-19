package com.kg.museumly.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class GoogleTokenVerificationClientImpl implements GoogleTokenVerificationClient {

    private final GoogleIdTokenVerifier verifier;

    public GoogleTokenVerificationClientImpl(GoogleIdTokenVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public GoogleIdToken verify(String idTokenString) throws GeneralSecurityException, IOException {
        return verifier.verify(idTokenString);
    }
}