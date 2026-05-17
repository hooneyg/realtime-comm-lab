package com.hooney.lab.realtime.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * 🔒 웹 애플리케이션 및 웹소켓 보안 설정 클래스
 * 
 * [Architecture Note]
 * 1. CSRF 비활성화: REST API 및 WebSocket 통신 환경에서의 간소화를 위해 비활성화합니다.
 * 2. 정적 자원 및 테스트 API 허용: 웹소켓 테스터 대시보드(ws-tester.html)와 테스트용 JWT 발급 API의 접근을 허용합니다.
 * 3. 소켓 엔드포인트 허용: WebSocket 연결 자체는 Spring Security 필터를 바이패스하고, Stomp Interceptor 단에서 JWT 검증을 처리하도록 합니다.
 * 4. CORS 설정: 로컬/도커 스케일아웃 환경에서 타 포트/도메인 요청이 자유롭게 가능하도록 전체 허용합니다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화 (테스트용이성 확보)
            .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS 위임
            .authorizeHttpRequests(auth -> auth
                // 대시보드 정적 HTML 및 리소스 허용
                .requestMatchers("/", "/index.html", "/ws-tester.html", "/static/**", "/webjars/**").permitAll()
                // 웹소켓 핸드쉐이크 엔드포인트 허용 (/ws-chat, /ws-rtc)
                .requestMatchers("/ws-chat/**", "/ws-rtc/**").permitAll()
                // 테스트용 JWT 토큰 발급 API 허용
                .requestMatchers("/api/v1/test/**").permitAll()
                // Swagger UI 관련 자원 허용
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable())); // SockJS 등과의 호환성을 위해 iframe 허용

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 개발 환경 편의를 위해 전면 개방
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
