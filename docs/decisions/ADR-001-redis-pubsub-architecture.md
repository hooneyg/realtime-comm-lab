# ADR 001: 실시간 분산 채팅 아키텍처로 Redis Pub/Sub 채택 (Adopting Redis Pub/Sub for Realtime Distributed Chat Architecture)

## 1. Status
**Accepted**

## 2. Context (배경)
웹소켓(WebSocket) 단일 서버 환경에서는 클라이언트의 세션 정보가 메모리에 유지되므로 메시징 처리에 문제가 없습니다.
하지만 서비스 트래픽 증가로 인해 서버 인스턴스가 여러 대(Scale-out)로 확장되는 엔터프라이즈 환경에서는,
서버 A에 붙은 유저와 서버 B에 붙은 유저가 실시간으로 채팅 메시지를 주고받아야 하는 요구사항이 발생합니다.
단순한 STOMP `SimpleBroker`는 단일 노드의 메모리에만 의존하므로 이를 해결할 수 없습니다.

## 3. Decision (결정)
외부 브로커(Message Broker) 역할을 수행할 수 있는 다양한 대안(RabbitMQ, Kafka, Redis) 중 **Redis Pub/Sub** 아키텍처를 채택합니다.

- **도입 기술:** Spring Boot 4.0.6 `spring-boot-starter-data-redis`, STOMP over WebSocket
- **구현 방식:**
  1. 클라이언트 메시지가 `ChatController`로 인입됨.
  2. 서버 타임스탬프 등 보정 로직 처리 후 `ChatMessagePublisher`를 통해 Redis의 공통 Topic(`chat-room`)으로 발행(Publish).
  3. 모든 애플리케이션 노드에 상주하는 `RedisMessageSubscriber`가 해당 Topic을 구독(Subscribe)하고 있다가 메시지를 수신.
  4. 각 노드의 Subscriber는 자신이 물고 있는(Connected) 로컬 WebSocket 클라이언트들에게 `SimpMessageSendingOperations`를 통해 브로드캐스팅.

## 4. Rationale (결정 이유)
- **Zero-Latency & High Throughput:** 채팅과 같은 실시간 양방향 통신 도메인에서는 "메시지 유실 방지"보다 "빠른 전달"이 핵심입니다. Redis Pub/Sub은 In-Memory 기반으로 초저지연(Zero-Latency) 라우팅을 제공하며, RabbitMQ나 Kafka 대비 설정과 관리가 압도적으로 가볍고 빠릅니다.
- **Stateless Web Nodes:** 웹 노드는 이제 상태(State)를 공유하지 않고 완전히 독립적인 확장이 가능해집니다.
- **Learning Curve & Ecosystem:** 기존 세션 스토어나 캐시로 Redis를 이미 인프라 덱에 포함하는 경우가 많아 추가 인프라 구성 부담이 적습니다.

## 5. Consequences (결과 및 고려사항)
- **메시지 영속성(Durability) 부재:** Redis Pub/Sub 구조상 Subscriber 노드가 잠시 다운된 사이에 발행된 메시지는 큐에 쌓이지 않고 휘발됩니다. (Fire and Forget)
- **Mitigation 전략:** 과거 채팅 기록이 보장되어야 하는 엔터프라이즈 채팅 도메인이라면, Redis Pub/Sub 전파와 동시에 RDBMS 또는 NoSQL(MongoDB/Cassandra)에 비동기 배치성으로 메시지를 Write-back 하거나 별도의 이벤트 버스(Kafka) 아키텍처를 혼합해야 합니다.
- (본 랩은 실시간 전달 보장에 중점을 두므로 영속화 로직은 `database-master-lab`의 범주로 위임합니다.)
