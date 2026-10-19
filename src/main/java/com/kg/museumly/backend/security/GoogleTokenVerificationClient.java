package com.kg.museumly.backend.security;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import java.io.IOException;
import java.security.GeneralSecurityException;

public interface GoogleTokenVerificationClient {
    GoogleIdToken verify(String idTokenString) throws GeneralSecurityException, IOException;
}