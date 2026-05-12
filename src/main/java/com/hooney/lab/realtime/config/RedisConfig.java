package com.hooney.lab.realtime.config;

import com.hooney.lab.realtime.chat.pubsub.RedisMessageSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Pub/Sub을 위한 코어 설정 클래스
 * 
 * [Architecture Note]
 * 실시간 채팅 서버가 10대(Scale-out)로 늘어났다고 가정해 봅시다.
 * A서버에 접속한 '철수'가 B서버에 접속한 '영희'에게 메시지를 보내려면 어떻게 해야 할까요?
 * 각 서버의 로컬 웹소켓 세션만으로는 서로 통신할 수 없습니다.
 * 
 * 해결책: 
 * 모든 서버가 중앙 집중된 Redis의 동일한 토픽("chat-room")을 바라보게 합니다(Subscribe).
 * '철수'가 보낸 메시지를 A서버가 Redis로 Publish하면,
 * B서버가 이를 수신(리스닝)하여 자신에게 연결된 '영희'의 웹소켓으로 쏴줍니다.
 * 이것이 엔터프라이즈 채팅 서버의 핵심인 분산 메시징(Pub/Sub) 아키텍처입니다.
 */
@Configuration
public class RedisConfig {

    public static final String CHAT_TOPIC = "chat-room";

    /**
     * 단일 토픽 설정
     * - 실제 서비스에서는 채팅방 ID 단위로 토픽을 동적 생성하거나 멀티플렉싱을 수행합니다.
     */
    @Bean
    public ChannelTopic channelTopic() {
        return new ChannelTopic(CHAT_TOPIC);
    }

    /**
     * Redis Pub/Sub 메시지를 수신하여 처리하는 컨테이너 설정
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListener(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter,
            ChannelTopic channelTopic) {
        
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // "chat-room" 토픽으로 들어오는 메시지를 리스너가 가로채도록 설정
        container.addMessageListener(listenerAdapter, channelTopic);
        return container;
    }

    /**
     * 실제 메시지를 수신했을 때 호출될 어댑터(리스너 객체와 메서드 매핑)
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisMessageSubscriber subscriber) {
        // RedisMessageSubscriber 클래스의 sendMessage 메서드를 콜백으로 지정
        return new MessageListenerAdapter(subscriber, "sendMessage");
    }

    /**
     * Redis 서버와 통신하기 위한 템플릿 설정
     * - 바이트 배열 대신 JSON 문자열 직렬화를 사용하여 사람이 읽을 수 있고 디버깅이 편하도록 설정합니다.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        
        // Key는 String 직렬화
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // Value는 JSON 직렬화
        redisTemplate.setValueSerializer(new Jackson2JsonRedisSerializer<>(Object.class));
        
        return redisTemplate;
    }
}
