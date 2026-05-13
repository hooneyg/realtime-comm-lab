package com.hooney.lab.realtime.chat.pubsub;

import tools.jackson.databind.ObjectMapper;
import com.hooney.lab.realtime.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

/**
 * [Inbound] Redis 토픽에 발행된 메시지를 수신(리스닝)하여 로컬 웹소켓 세션들로 브로드캐스트하는 서비스
 * 
 * [Architecture Note]
 * 이 클래스는 RedisConfig의 MessageListenerAdapter에 의해 등록됩니다.
 * 누군가(같은 서버든 다른 서버든) Redis에 메시지를 Publish하면,
 * 이 서버가 해당 메시지를 낚아채어(sendMessage 호출됨),
 * 현재 이 서버에 연결된 모든 STOMP 클라이언트(/topic/chat/room/{roomId})에게 쏴줍니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisMessageSubscriber {

    private final ObjectMapper objectMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Redis에서 메시지가 발행(Publish)되면 이 메서드가 실행됩니다.
     */
    public void sendMessage(String publishMessage) {
        try {
            // Redis에서 날아온 JSON 스트링을 다시 객체로 역직렬화
            ChatMessage chatMessage = objectMapper.readValue(publishMessage, ChatMessage.class);
            
            log.info("Redis Subscribe 수신 완료 -> 클라이언트로 전송 [Room: {}] message: {}", chatMessage.getRoomId(), chatMessage.getMessage());
            
            // 이 서버에 연결된 클라이언트 중, 해당 방(/topic/chat/room/{roomId})을 구독 중인 자들에게 뿌림
            messagingTemplate.convertAndSend("/topic/chat/room/" + chatMessage.getRoomId(), chatMessage);
            
        } catch (Exception e) {
            log.error("Redis Subscribe 메시지 역직렬화/전송 중 에러 발생: {}", e.getMessage(), e);
        }
    }
}
