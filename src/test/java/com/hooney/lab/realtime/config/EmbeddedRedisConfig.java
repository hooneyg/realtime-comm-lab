package com.hooney.lab.realtime.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import redis.embedded.RedisServer;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * 통합 테스트용 Embedded Redis 설정 (Test Configuration)
 * 
 * [Architecture Note]
 * 실시간 통신 서버의 심장부인 Redis Pub/Sub 로직을 테스트하려면 실제 Redis 인스턴스가 필요합니다.
 * CI 파이프라인이나 로컬 환경에서 별도의 Redis 설치 없이 즉시 통합 테스트를 수행할 수 있도록,
 * 프로세스 내부(메모리)에 경량화된 Redis 서버를 띄워 테스트 격리성을 보장합니다.
 */
@TestConfiguration
public class EmbeddedRedisConfig {

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() {
        try {
            // 운영체제 포트 충돌을 막기 위해 가급적 동적 포트를 사용하는 것이 좋으나,
            // 본 데모에서는 고정 포트 6379를 사용합니다.
            redisServer = new RedisServer(redisPort);
            redisServer.start();
        } catch (Exception e) {
            // 포트 충돌 시 이미 띄워져 있는 로컬 Redis를 사용하도록 예외 삼킴 (실무에서는 포트 동적 할당 로직 구현 필요)
        }
    }

    @PreDestroy
    public void stopRedis() {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory("localhost", redisPort);
    }
}
