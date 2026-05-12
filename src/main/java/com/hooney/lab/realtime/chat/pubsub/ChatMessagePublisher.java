package com.hooney.lab.realtime.chat.pubsub;

import com.hooney.lab.realtime.chat.dto.ChatMessage;
import com.hooney.lab.realtime.config.RedisConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * [Outbound] 클라이언트의 메시지를 Redis 서버(토픽)로 퍼블리시(발행)하는 서비스
 * 
 * [Architecture Note]
 * 클라이언트가 STOMP 서버로 메시지를 보내면,
 * 서버는 이 메시지를 즉시 로컬 세션들에게만 뿌리지 않습니다.
 * 대신 이 Publisher를 통해 중앙 Redis의 "chat-room" 토픽으로 던집니다.
 * 이로써 서버 A에 붙은 유저의 메시지가 서버 B, C, D로 모두 뻗어나갈 수 있는 트리거가 됩니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessagePublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publish(ChatMessage message) {
        log.info("Redis Publish [Topic: {}] sender: {}, message: {}", RedisConfig.CHAT_TOPIC, message.getSender(), message.getMessage());
        
        // 설정해둔 채널(토픽)으로 메시지 객체를 던집니다. (JSON으로 자동 직렬화됨)
        redisTemplate.convertAndSend(RedisConfig.CHAT_TOPIC, message);
    }
}
