package com.springboot.jenka_coffee.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

public interface GoogleOAuthService {
    GoogleIdToken.Payload verifyIdToken(String idToken);
}
