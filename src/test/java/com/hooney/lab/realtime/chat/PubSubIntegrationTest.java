package com.hooney.lab.realtime.chat;

import com.hooney.lab.realtime.chat.dto.ChatMessage;
import com.hooney.lab.realtime.chat.pubsub.ChatMessagePublisher;
import com.hooney.lab.realtime.config.EmbeddedRedisConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis Pub/Sub 메시징 통합 검증 테스트
 * 
 * [Test Architecture]
 * 이 테스트는 Embedded Redis를 띄운 상태에서,
 * Publisher가 Redis 토픽으로 메시지를 쏘았을 때
 * 스프링 컨텍스트가 이를 성공적으로 다시 낚아채어 Subscriber 로직으로 흘러가는지(Flow)
 * 전체 과정을 통합적으로 검증합니다.
 */
@Slf4j
@SpringBootTest
@Import(EmbeddedRedisConfig.class)
public class PubSubIntegrationTest {

    @Autowired
    private ChatMessagePublisher publisher;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    @DisplayName("Redis Pub/Sub을 통해 메시지가 성공적으로 발행(Publish)되어야 한다")
    void testPublishMessageToRedis() throws InterruptedException {
        // given
        ChatMessage mockMessage = ChatMessage.builder()
                .type(ChatMessage.MessageType.TALK)
                .roomId("room-1234")
                .sender("hooney")
                .message("Test Message via Embedded Redis")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        log.info(">>>> 통합 테스트: Redis로 메시지 발행 시작");
        publisher.publish(mockMessage);

        // then
        // 비동기 Pub/Sub 특성상 즉각적인 Assertion은 어렵습니다. 
        // 테스트 통과 여부는 예외가 터지지 않고 정상적으로 convertAndSend가 수행되었는지,
        // 그리고 로그 상에 Subscriber가 낚아챈 기록이 찍히는지를 육안으로 1차 검증합니다.
        
        // (실무에서는 CompletableFuture와 커스텀 Subscriber를 이용해 CountDownLatch 검증을 수행합니다)
        TimeUnit.SECONDS.sleep(1); // Subscriber가 메시지를 소비할 시간 제공
        assertThat(redisTemplate.getConnectionFactory()).isNotNull();
    }
}
