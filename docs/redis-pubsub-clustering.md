# 📦 Redis Pub/Sub Clustering (레디스 펍/섭 클러스터링)

이 문서는 실시간 분산 메시징의 릴레이 계층으로 사용되는 Redis Pub/Sub의 동작 원리, 클러스터(Cluster) 및 센티널(Sentinel) 환경에서의 전파 메커니즘, 그리고 고가용성(High Availability) 튜닝 전략을 설명합니다.

---

## 📌 1. Redis Pub/Sub의 기본 설계 사상 (Fire-and-Forget)

Redis Pub/Sub은 고성능의 **"발행 및 망각(Fire-and-Forget)"** 모델을 따릅니다.
- **메모리 저장소 미사용**: 일반적인 Redis Key-Value 구조나 Message Queue(예: RabbitMQ, Kafka)와 달리, 채널에 메시지가 발행되면 수신 대기 중인 활성화된 구독자(Subscriber)에게 즉시 전달한 뒤 **메시지를 즉시 소멸**시킵니다.
- **오프라인 메시지 유실**: 구독을 잠시 중단하거나 서버 점검으로 세션이 끊어졌던 클라이언트는 커넥션이 없던 시점에 발행된 과거 메시지를 다시 받아볼 수 없습니다.
- **용도**: 과거 이력 저장이 불필요하고, 오직 1초 미만의 실시간 전파 속도가 중요한 분산 노드 간의 "상태 동기화"나 "실시간 브로드캐스팅(Realtime Broadcasting)"에 적합합니다.

---

## 🏗️ 2. 클러스터 환경에서의 Pub/Sub 전파 메커니즘

레디스 클러스터(Redis Cluster) 환경에서 Pub/Sub은 해시 슬롯(Hash Slot) 제약과 상관없이 **전체 클러스터 범위**로 브로드캐스트됩니다.

```text
[클러스터 노드 1] ── (Publish "Hello" to "room-1")
       │
       ├─── [메시지 자동 포워딩 (Cluster Bus)] ───► [클러스터 노드 2] (Subscribe "room-1")
       │                                                │ (클라이언트 푸시)
       └─── [메시지 자동 포워딩 (Cluster Bus)] ───► [클러스터 노드 3] (Subscribe 없음)
```

1. **글로벌 브로드캐스트(Global Broadcast)**: 클라이언트가 클러스터 내의 임의의 Master Node에 `PUBLISH` 명령을 보내면, 해당 노드는 내부 클러스터 버스(Cluster Bus)를 통해 클러스터 내의 **모든 다른 Master Node**로 메시지를 자동으로 포워딩합니다.
2. **트래픽 오버헤드 주의**: 클러스터 내의 노드 개수가 너무 많아지면(예: 수십 개 이상), 하나의 Publish 이벤트가 모든 노드로 복제되어 네트워크 대역폭(Network Bandwidth) 과부하가 발생할 수 있습니다. 
3. **해결 전략 (Redis 7.0+)**:
   - Redis 7.0 버전부터 지원하는 **Sharded Pub/Sub**(`SPUBLISH` / `SSUBSCRIBE`)을 활용하면, 채널명이 매핑되는 특정 해시 슬롯의 노드와 해당 복제본(Replica) 노드 사이에서만 메시지를 전송하도록 제한하여 대규모 클러스터에서의 오버헤드를 극복할 수 있습니다.

---

## ⚙️ 3. 운영 환경 고가용성(HA) 구성 방식

실시간 채팅 서비스의 무중단 운영을 위해 다음과 같은 구성을 권장합니다.

| 아키텍처 패턴 | 장점 | 단점 / 고려사항 |
| :--- | :--- | :--- |
| **Sentinel (센티널)** | Master 장애 시 자동 페일오버(Failover). 단일 채널 Pub/Sub 성능 극대화 | 샤딩이 지원되지 않으므로 단일 노드 트래픽 한계에 도달 가능 |
| **Cluster (클러스터)** | 분산 샤딩 지원, 데이터 쓰기 성능 분산 | 노드가 많을 때 복제 트래픽 증가 (Redis 7.0+ Sharded Pub/Sub 권장) |

### 🛠️ 가용성 확보를 위한 Spring Boot 커넥션 설정
- Redis Sentinel을 연동할 때 `LettuceConnectionFactory` 설정을 센티널 주소 정보와 마스터 ID(`masterId`)로 구성합니다.
- 네트워크 단절 및 재연결 상황에서 리스너가 예외 없이 재연결 상태를 유지하도록 `LettuceClientConfiguration`에 적절한 KeepAlive 설정을 유지합니다.
