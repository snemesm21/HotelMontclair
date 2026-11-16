package com.example.demo.service;

public interface GoogleTokenValidator {
    GoogleUserInfo validate(String idToken, String expectedNonce);

    record GoogleUserInfo(String subject, String email, String firstName, String lastName, String avatarUrl) {
    }
}
