package com.hooney.lab.realtime.chat.controller;

import com.hooney.lab.realtime.chat.dto.ChatMessage;
import com.hooney.lab.realtime.chat.pubsub.ChatMessagePublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.time.Instant;

/**
 * 클라이언트로부터 STOMP 메시지를 수신하는 엔드포인트 컨트롤러
 * 
 * [Architecture Note]
 * 클라이언트가 `/app/chat/message` 경로로 메시지를 전송하면 이 컨트롤러가 받습니다.
 * 이 컨트롤러의 유일한 역할은 '비즈니스 로직(필터링, 타임스탬프 주입 등)을 수행한 뒤,
 * 로컬 클라이언트들에게 바로 쏴주는 것이 아니라 **Redis로 Publish**하는 것'입니다.
 * 분산 처리는 Publisher와 Subscriber에게 온전히 위임합니다.
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatController {

    private final ChatMessagePublisher chatMessagePublisher;

    /**
     * websocket "/app/chat/message"로 들어오는 메시징을 처리한다.
     */
    @MessageMapping("/chat/message")
    public void handleChatMessage(ChatMessage message) {
        log.info("STOMP 클라이언트로부터 메시지 수신: sender={}, message={}", message.getSender(), message.getMessage());

        // 입장 메시지 처리 로직
        if (ChatMessage.MessageType.ENTER.equals(message.getType())) {
            message = ChatMessage.builder()
                    .type(ChatMessage.MessageType.ENTER)
                    .roomId(message.getRoomId())
                    .sender("[System]")
                    .message(message.getSender() + "님이 방에 입장하셨습니다.")
                    .timestamp(Instant.now().toEpochMilli())
                    .build();
        } else {
            // 메시지에 현재 서버 타임스탬프를 보정하여 주입 (클라이언트 시간 위조 방지)
            message = ChatMessage.builder()
                    .type(message.getType())
                    .roomId(message.getRoomId())
                    .sender(message.getSender()) // 실제로는 JWT 인터셉터에서 넘긴 Authentication을 사용해야 안전함
                    .message(message.getMessage())
                    .timestamp(Instant.now().toEpochMilli())
                    .build();
        }

        // 1. 비즈니스 처리 완료
        // 2. 다른 모든 분산 서버가 볼 수 있도록 Redis로 Publish 던짐
        chatMessagePublisher.publish(message);
    }
}
