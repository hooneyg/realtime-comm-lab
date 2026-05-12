package com.hooney.lab.realtime.config;

import com.hooney.lab.realtime.security.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 및 STOMP 메시지 브로커 설정
 * 
 * [Architecture Note]
 * 1. Endpoint: 클라이언트가 소켓 연결을 맺는 진입점 (/ws-chat, /ws-rtc).
 * 2. Message Broker: 클라이언트 간 메시지를 라우팅하는 브로커. 
 *    향후 Redis Pub/Sub과 연동되어 스케일 아웃 환경에서도 완벽하게 동작하도록 구성됩니다.
 * 3. ChannelInterceptor: CONNECT 시점의 JWT 보안 인증을 위해 Interceptor를 주입합니다.
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 1. 텍스트 기반 채팅을 위한 엔드포인트
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*") // 실무에서는 정확한 도메인을 지정해야 합니다.
                .withSockJS(); // 구버전 브라우저 지원을 위한 SockJS 폴백

        // 2. WebRTC 화상 시그널링을 위한 엔드포인트
        registry.addEndpoint("/ws-rtc")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 발행(Publish) 요청의 prefix: /app 경로로 들어온 메시지는 @MessageMapping 컨트롤러로 라우팅됨
        registry.setApplicationDestinationPrefixes("/app");
        
        // 메시지 구독(Subscribe) 요청의 prefix: /topic (브로드캐스트 1:N), /queue (1:1 메시지)
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // 클라이언트로부터 들어오는 메시지 채널에 JWT 보안 검증 인터셉터 등록
        registration.interceptors(jwtChannelInterceptor);
    }
}
