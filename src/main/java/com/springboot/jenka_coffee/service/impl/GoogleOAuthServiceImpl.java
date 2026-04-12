package com.springboot.jenka_coffee.service.impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.springboot.jenka_coffee.service.GoogleOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    @Value("${google.oauth.client-id}")
    private String clientId;
    
    // VULN-HTTP-CONNECTION-POOL-EXHAUSTION FIX: Reuse verifier instance
    // Creating new NetHttpTransport for each verification leaks HTTP connections
    private final GoogleIdTokenVerifier verifier;
    
    public GoogleOAuthServiceImpl(@Value("${google.oauth.client-id}") String clientId) {
        this.clientId = clientId;
        // Initialize verifier once as singleton - reuse across all verifications
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    @Override
    public GoogleIdToken.Payload verifyIdToken(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);
            if (googleIdToken != null) {
                return googleIdToken.getPayload();
            } else {
                log.warn("Invalid Google ID token");
                return null;
            }
        } catch (Exception e) {
            log.error("Error verifying Google ID token: {}", e.getMessage());
            return null;
        }
    }
}
