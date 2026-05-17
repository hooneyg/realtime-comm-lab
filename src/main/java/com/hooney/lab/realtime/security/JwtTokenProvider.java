package com.hooney.lab.realtime.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * JWT 검증 및 파싱을 담당하는 유틸리티 컴포넌트
 * 
 * [Security Note]
 * MSA 환경에서는 security-auth-core에서 발급한 JWT를
 * Gateway가 1차 검증하고, 내부 서비스인 realtime-comm-lab에서도
 * 이 유틸리티를 통해 웹소켓 핸드쉐이크 시 2차 검증을 수행합니다.
 * 이를 통해 비인가 사용자의 악의적인 소켓 연결을 원천 차단합니다.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;

    public JwtTokenProvider(@Value("${jwt.secret:default-secret-key-must-be-long-enough-for-hs256}") String secretKey) {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 토큰의 유효성을 검증합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            log.error("유효하지 않은 JWT 토큰입니다: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 테스트 및 모킹 검증을 위한 JWT 토큰 생성 (1시간 유효)
     */
    public String createToken(String username) {
        java.util.Date now = new java.util.Date();
        java.util.Date expiry = new java.util.Date(now.getTime() + 3600000); // 1시간 유효

        return Jwts.builder()
                .subject(username)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /**
     * 검증된 토큰에서 Authentication 객체를 추출합니다.
     * STOMP 세션의 User Principal로 등록되어 라우팅 보안에 활용됩니다.
     */
    public Authentication getAuthentication(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        String username = claims.getSubject();
        // 실제 환경에서는 권한(Roles) 정보도 파싱하여 주입합니다.
        return new UsernamePasswordAuthenticationToken(username, "", Collections.emptyList());
    }
}
