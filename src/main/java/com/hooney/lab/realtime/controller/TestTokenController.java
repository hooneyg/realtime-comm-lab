package com.hooney.lab.realtime.controller;

import com.hooney.lab.realtime.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 🎫 테스트용 JWT 토큰 발급 REST 컨트롤러
 * 
 * [Architecture Note]
 * 이 컨트롤러는 로컬 개발 및 E2E 테스트 과정에서 
 * Stomp WebSocket Handshake 시 인증을 무사히 통과할 수 있도록
 * 손쉬운 원클릭 JWT 토큰 생성을 보조하는 도우미 API입니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TestTokenController {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 특정 사용자명으로 1시간짜리 유효한 테스트 JWT 발급
     * 
     * [HTTP Request]
     * GET /api/v1/test/token?username=user-A
     * 
     * [HTTP Response]
     * {
     *   "username": "user-A",
     *   "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOi..."
     * }
     */
    @GetMapping("/token")
    public Map<String, String> generateToken(@RequestParam(defaultValue = "test-user") String username) {
        log.info("📢 테스트 JWT 토큰 발급 요청 수신: username={}", username);
        String token = jwtTokenProvider.createToken(username);
        return Map.of(
                "username", username,
                "token", token
        );
    }
}
