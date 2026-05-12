package com.hooney.lab.realtime.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 단위 테스트
 * 
 * [Test Architecture]
 * STOMP 연결 시 사용될 토큰 검증 로직이 정확히 동작하는지 검증합니다.
 * 외부 의존성 없이 순수 로직만 검증하여 테스트 속도를 극대화합니다.
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private final String secretKey = "this-is-a-very-long-secret-key-for-test-only-do-not-use-in-prod";

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(secretKey);
    }

    @Test
    @DisplayName("유효한 JWT 토큰을 검증하면 true를 반환한다")
    void validateToken_ValidToken() {
        // given
        String validToken = Jwts.builder()
                .subject("hooney")
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60)) // 1분 뒤 만료
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        // when
        boolean isValid = jwtTokenProvider.validateToken(validToken);

        // then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("잘못된 서명의 JWT 토큰을 검증하면 false를 반환한다")
    void validateToken_InvalidSignature() {
        // given
        String invalidToken = Jwts.builder()
                .subject("hooney")
                .signWith(Keys.hmacShaKeyFor("wrong-secret-key-that-is-very-long-enough".getBytes(StandardCharsets.UTF_8)))
                .compact();

        // when
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);

        // then
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("토큰에서 올바른 Authentication 객체를 추출한다")
    void getAuthentication() {
        // given
        String username = "hooney_admin";
        String token = Jwts.builder()
                .subject(username)
                .signWith(Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        // when
        Authentication authentication = jwtTokenProvider.getAuthentication(token);

        // then
        assertThat(authentication.getName()).isEqualTo(username);
    }
}
