package com.legendme.app.domain.model;

public record  AuthResult (
        String AccessToken,
        String refreshToken,
        String userId,
        String email,
        String name
) {}
