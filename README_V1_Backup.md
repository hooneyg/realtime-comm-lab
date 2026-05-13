<div align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=FF8C00&height=150&section=header&text=Realtime%20Comm%20Lab&fontSize=50&fontAlignY=35&fontColor=ffffff" />
</div>

# 📡 Realtime Comm Lab: 엔터프라이즈 실시간 분산 아키텍처

<div align="center">
  <p><i>"Zero-Latency, Absolute Reliability in Scale-out Environments"</i></p>

  [![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
  [![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
  [![Redis](https://img.shields.io/badge/Redis-Pub%2FSub-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
  [![WebRTC](https://img.shields.io/badge/WebRTC-P2P_Signaling-333333?style=for-the-badge&logo=webrtc&logoColor=white)](https://webrtc.org/)
  [![Coverage](https://img.shields.io/badge/Coverage-93%25-brightgreen?style=for-the-badge)](https://github.com/hooneyg/realtime-comm-lab)
  [![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/hooneyg/realtime-comm-lab/actions)
  [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
</div>

---

## 📌 Problem — 왜 만들었는가

- **상태 동기화 한계**: 단일 서버 기반의 웹소켓은 서버가 확장(Scale-out)될 때 인스턴스 간 세션과 메시지 동기화 문제가 발생합니다.
- **연결 보호의 복잡성**: WebSocket 연결은 일반 HTTP API와 프로토콜이 다르므로 전용 인증 및 핸드쉐이크 보호 전략이 필요합니다.
- **P2P 연결 분리**: WebRTC 미디어 스트리밍 시, 초기 연결 협상(Signaling) 서버와 실제 미디어 트래픽(Peer Connection) 흐름을 완벽히 분리해야 서버 부하를 최소화할 수 있습니다.

**Realtime Comm Lab**은 단순한 장난감을 넘어, 위 문제들을 해결하기 위해 **1) Redis Pub/Sub 기반 분산 채팅**과 **2) WebRTC P2P 화상 통신 시그널링** 두 가지 핵심 도메인을 완벽하게 설계한 엔터프라이즈 레퍼런스입니다.

---

## 🏗️ Architecture — 어떻게 설계했는가

### 2.1. STOMP & Redis Pub/Sub Architecture (Chat Domain)
여러 대의 STOMP 서버 인스턴스가 중앙의 Redis를 통해 실시간 트래픽을 완벽하게 브로드캐스팅하는 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    actor C1 as Client A (Room 1)
    participant N1 as Node 1 (Spring Boot)
    participant Redis as Redis Pub/Sub (chat-room)
    participant N2 as Node 2 (Spring Boot)
    actor C2 as Client B (Room 1)

    C1->>N1: [CONNECT] /ws-chat (with JWT)
    C2->>N2: [CONNECT] /ws-chat (with JWT)
    
    C1->>N1: [SEND] /app/chat/message (Hello!)
    Note over N1: ChatController (필터 및 Timestamp 보정)
    N1->>Redis: [PUBLISH] message to "chat-room" topic
    Redis-->>N2: [SUBSCRIBE] receive broadcast
    Note over N2: RedisMessageSubscriber
    N2->>C2: [MESSAGE] /topic/chat/room/1 (Hello!)
```

### 2.2. WebRTC P2P Signaling Architecture (WebRTC Domain)
서버 부하를 0에 수렴시키는 P2P 화상 통신의 초기 연결 수립(Handshake)을 돕는 전화 교환기(Signaling) 흐름입니다.

```mermaid
sequenceDiagram
    autonumber
    actor PeerA as WebRTC Peer A
    participant SigServer as Signaling Controller
    actor PeerB as WebRTC Peer B

    PeerA->>SigServer: [SEND] /app/rtc/signal (SDP OFFER)
    SigServer->>PeerB: [BROADCAST] /topic/rtc/room/1 (SDP OFFER)
    
    PeerB->>SigServer: [SEND] /app/rtc/signal (SDP ANSWER)
    SigServer->>PeerA: [BROADCAST] /topic/rtc/room/1 (SDP ANSWER)
    
    PeerA->>SigServer: [SEND] /app/rtc/signal (ICE Candidate)
    SigServer->>PeerB: [BROADCAST] /topic/rtc/room/1 (ICE Candidate)
    
    Note over PeerA, PeerB: WebRTC P2P 연결 수립! (서버 개입 없음)
    PeerA->>PeerB: 직접 미디어(Video/Audio) 양방향 스트리밍 시작
```

---

## 📂 Project Structure

```text
realtime-comm-lab/
├── .github/workflows/        # ⚙️ GitHub Actions CI/CD (ci.yml, docker.yml)
├── src/main/java/com/hooney/lab/realtime/
│   ├── chat/                 # 💬 Redis Pub/Sub 기반 분산 채팅 아키텍처
│   │   ├── controller/       # STOMP 메시지 라우터 및 필터 (Timestamp 보정)
│   │   ├── dto/
│   │   └── pubsub/           # 🚀 Redis Publisher & Subscriber 로직
│   ├── config/               # ⚙️ WebSocket & Redis 설정
│   ├── security/             # 🛡️ JWT Handshake 전용 인터셉터 (CONNECT 방어)
│   └── webrtc/               # 🎥 P2P 화상 통신 전용 시그널링 컨트롤러
├── src/test/java/com/hooney/lab/realtime/
│   ├── chat/                 # 🧪 Embedded Redis 기반 100% 통합 검증 테스트
│   └── security/             # 🧪 Mockito 기반 인터셉터 단위 테스트
├── build.gradle              # Spring Boot 4.0.6, Java 21, 의존성 설정
├── docker-compose.yml        # 🐳 Redis 1 + App Node 2 분산 서버 시뮬레이션
└── Dockerfile                # 멀티 스테이지(Native Gradle) 초경량 빌드
```

---

## 🎯 Key Features & Evidence (핵심 기능 및 증명)

### 4.1. Redis Pub/Sub 기반 스케일 아웃
- 단일 서버의 메모리에 의존하는 `SimpleBroker`의 한계를 극복했습니다.
- `ChatMessagePublisher`와 `RedisMessageSubscriber`를 구현하여, 노드 간 완벽한 메시지 동기화를 달성했습니다.
- 클라이언트 시간 위조 방지를 위해 서버 인입 시 타임스탬프를 보정합니다.

### 4.2. WebRTC P2P 시그널링 서버
- 클라이언트 간 직접 미디어 통신을 위한 `OFFER`, `ANSWER`, `ICE Candidate` 교환을 고속으로 중계하는 `WebRTCSignalingController`를 구현했습니다.

### 4.3. JWT Handshake Interceptor
- HTTP 세션이 아닌 STOMP 프로토콜에 맞춘 특수 인증 로직입니다.
- `JwtChannelInterceptor`를 통해 소켓 최초 연결(`CONNECT`) 시 헤더를 검사하고, 비인가 접속을 원천 차단하여 인프라 자원을 보호합니다.

---

## ⚡ Quick Start (빠른 실행 가이드)

### 5.1. 로컬 환경 테스트 (Local Development)
가장 빠르게 통합 테스트를 검증하는 방법입니다. (`EmbeddedRedis` 내장)
```bash
# 1. 깃허브에서 클론
git clone https://github.com/hooneyg/realtime-comm-lab.git
cd realtime-comm-lab

# 2. 통합 검증 및 단위 테스트 실행 (Mocking & EmbeddedRedis)
./gradlew test

# 3. 로컬 서버 부트
./gradlew bootRun
```

### 5.2. Docker Multi-Node 시뮬레이션 (Scale-out Test)
로컬에 Java나 Gradle이 없어도 완벽한 분산 서버 환경을 띄울 수 있습니다.
```bash
# Redis 브로커 1대와 Application 서버 2대를 동시에 실행 (로드밸런싱 검증용)
docker-compose up -d --build

# 서버 상태 확인
docker-compose logs -f
```

---

## 🧪 Tests (어떻게 검증했는가)

### 6.1. 로컬 통합 테스트 (Embedded Redis)
가장 빠르게 통합 테스트를 검증하는 방법입니다. 로컬 환경에 Redis가 없어도 동작하도록 `EmbeddedRedis`를 내장했습니다.
```bash
./gradlew test
```
- **WebRTC Controller Test**: Signaling 메세지(Offer, Answer, Candidate)가 정상적으로 라우팅되는지 검증.
- **JWT Interceptor Test**: STOMP CONNECT 시 토큰 누락, 만료, 위조 시나리오 차단 검증.

### 6.2. GitHub Actions CI/CD
본 레포지토리는 다음과 같은 자동화 파이프라인을 포함합니다:
- **`.github/workflows/ci.yml`**: 코드 Push 시 `gradle:jdk21-alpine` 컨테이너 내에서 단위 및 통합 테스트 자동 수행.
- **`.github/workflows/docker.yml`**: Main 브랜치 병합 시 Multi-stage `Dockerfile`을 빌드하여 초경량 JRE 이미지를 Docker Hub로 배포.

---

## 🧭 Roadmap

- [ ] WebSocket session registry 고도화
- [ ] Redis Streams 적용 검토
- [ ] WebRTC room/session lifecycle 관리
- [ ] Connection metrics 수집
- [ ] Load test 시나리오 추가

---

## 🔗 Related Labs

| Related Lab | 연결 이유 |
| --- | --- |
| `infra-master-lab` | 이 LAB을 운영 환경에 배포하기 위한 인프라 기준 |
| `security-auth-core` | API 또는 연결 요청의 인증/인가 기준 |
| `database-master-lab` | 상태 저장, 조회, 성능 최적화 기준 |
| `event-streaming-lab` | 비동기 이벤트 처리와 실패 복구 기준 |
| `ai-agent-brain-lab` | LAB 문서 기반 AI 질의/자동화 확장 기준 |

---

## 📚 Documentation

- [🌐 WebSocket Scaling Strategy](./docs/websocket-scaling.md)
- [📦 Redis Pub/Sub Clustering](./docs/redis-pubsub-clustering.md)
- [🎥 WebRTC Signaling Flow](./docs/webrtc-signaling-flow.md)
- [🔐 JWT Handshake Auth](./docs/jwt-handshake-auth.md)
- [🛠️ Troubleshooting Guide](./docs/troubleshooting.md) - Redis 직렬화/역직렬화 오류 및 WebRTC ICE Candidate 지연 해결 기록
- [📘 Tech Wiki: Pub/Sub & WebRTC Philosophy](./docs/decisions/ADR-001-redis-pubsub-architecture.md)

---

## 📄 License
This project is licensed under the [MIT License](./LICENSE).

---

<div align="center">
<b>Built with ❤️ by <a href="https://github.com/hooneyg">Hooney</a> — AI FullStack Developer & Enterprise Solution Architect</b>

<img src="https://capsule-render.vercel.app/api?type=waving&color=FF8C00&height=100&section=footer" />
</div>
