package com.company.keyvault.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {
    private final JwtTokenProvider provider = new JwtTokenProvider(
            "test-secret-that-is-long-enough-for-token-tests", 60_000, 120_000);

    @Test
    void accessAndRefreshTokensHaveDifferentTypes() {
        String access = provider.generateAccessToken("admin@example.com");
        String refresh = provider.generateRefreshToken("admin@example.com");

        assertTrue(provider.validateToken(access));
        assertTrue(provider.isAccessToken(access));
        assertFalse(provider.isRefreshToken(access));
        assertTrue(provider.isRefreshToken(refresh));
        assertFalse(provider.isAccessToken(refresh));
    }
}
