package com.innowise.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private static final String TEST_SECRET = "test-jwt-secret-key-for-unit-tests-only-12345";
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider(TEST_SECRET);
    }

    private String generateToken(Long userId, String role) {
        Key key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .signWith(key)
                .compact();
    }

    @Test
    void shouldReturnTrue_whenTokenIsValid() {
        String token = generateToken(1L, "ADMIN");

        boolean result = jwtProvider.validateToken(token);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenTokenIsInvalid() {
        boolean result = jwtProvider.validateToken("invalid.token");

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsEmpty() {
        boolean result = jwtProvider.validateToken("");

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsNull() {
        boolean result = jwtProvider.validateToken(null);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalse_whenTokenIsMalformed() {
        boolean result = jwtProvider.validateToken("abc.def");

        assertThat(result).isFalse();
    }

    @Test
    void shouldExtractUserIdFromToken() {
        String token = generateToken(123L, "ADMIN");

        Long userId = jwtProvider.getUserId(token);

        assertThat(userId).isEqualTo(123L);
    }

    @Test
    void shouldExtractRoleFromToken() {
        String token = generateToken(123L, "ADMIN");

        String role = jwtProvider.getRole(token);

        assertThat(role).isEqualTo("ADMIN");
    }
}
