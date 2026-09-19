package ca.umika.api.auth;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtUtilTest {

    private static final String SECRET = "temporary-test-secret-temporary-test-secret-temporary-test-secret";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
    }

    @Test
    void generatedTokenHasNoExpirationAndRemainsValid() {
        String token = jwtUtil.generateToken("customer@example.com");
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        assertThat(claims.getSubject()).isEqualTo("customer@example.com");
        assertThat(claims.getExpiration()).isNull();
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
    }

    @Test
    void rejectsTokenWithInvalidSignature() {
        String token = jwtUtil.generateToken("customer@example.com");
        ReflectionTestUtils.setField(jwtUtil, "secret", "different-test-secret-different-test-secret-different-test-secret");

        assertThat(jwtUtil.isTokenValid(token)).isFalse();
    }
}
