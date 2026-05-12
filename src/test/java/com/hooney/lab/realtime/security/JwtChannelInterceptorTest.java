package com.hooney.lab.realtime.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * JwtChannelInterceptor 단위 테스트
 * 
 * [Test Architecture]
 * STOMP CONNECT 커맨드가 인입되었을 때, 인터셉터가 정확하게
 * Authorization 헤더를 까보고 토큰을 검증하는지 Mocking을 통해 테스트합니다.
 */
@ExtendWith(MockitoExtension.class)
class JwtChannelInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private JwtChannelInterceptor jwtChannelInterceptor;

    @Test
    @DisplayName("CONNECT 커맨드이고 유효한 JWT 토큰이 있으면 세션에 Authentication을 할당한다")
    void preSend_ValidJwtOnConnect() {
        // given
        String token = "valid-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        Authentication auth = new UsernamePasswordAuthenticationToken("hooney", "", Collections.emptyList());
        given(jwtTokenProvider.validateToken(token)).willReturn(true);
        given(jwtTokenProvider.getAuthentication(token)).willReturn(auth);

        MessageChannel channel = mock(MessageChannel.class);

        // when
        Message<?> resultMessage = jwtChannelInterceptor.preSend(message, channel);

        // then
        StompHeaderAccessor resultAccessor = StompHeaderAccessor.getAccessor(resultMessage, StompHeaderAccessor.class);
        assertThat(resultAccessor).isNotNull();
        assertThat(resultAccessor.getUser()).isEqualTo(auth);
    }

    @Test
    @DisplayName("CONNECT 커맨드인데 JWT 토큰이 유효하지 않으면 예외가 발생한다")
    void preSend_InvalidJwtOnConnect() {
        // given
        String token = "invalid-token";
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.addNativeHeader("Authorization", "Bearer " + token);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        given(jwtTokenProvider.validateToken(token)).willReturn(false);
        MessageChannel channel = mock(MessageChannel.class);

        // when & then
        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid JWT token");
    }

    @Test
    @DisplayName("CONNECT 커맨드인데 Authorization 헤더가 없으면 예외가 발생한다")
    void preSend_MissingHeaderOnConnect() {
        // given
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        MessageChannel channel = mock(MessageChannel.class);

        // when & then
        assertThatThrownBy(() -> jwtChannelInterceptor.preSend(message, channel))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Missing Authorization header");
    }
}
