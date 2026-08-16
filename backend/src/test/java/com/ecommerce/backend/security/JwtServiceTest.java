package com.ecommerce.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Regression test for a real deploy failure: a hosting platform's env var UI
 * embedded a stray newline in JWT_SECRET, which Java's strict Base64 decoder
 * rejected outright and crashed the app at startup. JwtService now strips all
 * whitespace before decoding — this pins that down so it can't quietly regress.
 */
class JwtServiceTest {

    private static final String VALID_SECRET =
            "c2VjdXJlLWVjb21tZXJjZS1qd3Qtc2VjcmV0LWtleS1jaGFuZ2UtbWUtaW4tcHJvZHVjdGlvbi1wbGVhc2U=";

    private final UserDetails user = new User("rahul@example.com", "irrelevant", java.util.List.of());

    @Test
    void constructingWithAnEmbeddedNewline_doesNotThrow() {
        String secretWithEmbeddedNewline = VALID_SECRET.substring(0, 20) + "\n" + VALID_SECRET.substring(20);

        assertThatCode(() -> new JwtService(secretWithEmbeddedNewline, 86_400_000L))
                .doesNotThrowAnyException();
    }

    @Test
    void constructingWithLeadingOrTrailingWhitespace_doesNotThrow() {
        assertThatCode(() -> new JwtService("  " + VALID_SECRET + "\n", 86_400_000L))
                .doesNotThrowAnyException();
    }

    @Test
    void aTokenGeneratedWithAWhitespaceContaminatedSecret_stillValidatesCorrectly() {
        JwtService jwtService = new JwtService(VALID_SECRET + "\n", 86_400_000L);

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("rahul@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}
