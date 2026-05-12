package com.hooney.lab.realtime.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * WebSocket(STOMP) 채널에 들어오는 메시지를 가로채어 JWT 인증을 수행하는 인터셉터
 * 
 * [Architecture Note]
 * 일반적인 HTTP 요청과 달리, WebSocket은 최초 Handshake 이후 연결이 지속되므로
 * STOMP의 CONNECT 커맨드 시점에 Authorization 헤더를 검사해야 합니다.
 * JWT가 유효하지 않으면 연결 자체를 거절하여 서버 리소스 낭비와 보안 위협을 방지합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // STOMP 헤더에서 Authorization 추출 (예: Bearer <token>)
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtTokenProvider.validateToken(token)) {
                    // 유효한 토큰일 경우 Authentication 객체를 세션에 할당
                    Authentication authentication = jwtTokenProvider.getAuthentication(token);
                    accessor.setUser(authentication);
                    log.info("WebSocket CONNECT 성공: User={}", authentication.getName());
                } else {
                    log.warn("WebSocket CONNECT 거절: 유효하지 않은 JWT 토큰");
                    throw new IllegalArgumentException("Invalid JWT token");
                }
            } else {
                log.warn("WebSocket CONNECT 거절: Authorization 헤더 누락");
                throw new IllegalArgumentException("Missing Authorization header");
            }
        }
        return message;
    }
}
