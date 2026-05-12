package com.hooney.lab.realtime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Realtime Communication Lab 애플리케이션 진입점
 * 
 * [Architecture Note]
 * 이 서버는 대규모 동시 접속자 처리를 위해 무상태(Stateless)로 설계됩니다.
 * 서버 인스턴스가 여러 대(Scale-out) 배포되더라도, WebSocket 세션 간의 메시징은
 * Redis Pub/Sub을 통해 완벽하게 동기화됩니다.
 */
@SpringBootApplication
public class RealtimeApplication {
    public static void main(String[] args) {
        SpringApplication.run(RealtimeApplication.class, args);
    }
}
