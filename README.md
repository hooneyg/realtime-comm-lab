<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=FF8C00&height=200&section=header&text=Realtime%20Comm%20Lab&fontSize=50&animation=fadeIn&fontAlignY=38&fontColor=FFFFFF" />

<h3>📡 Enterprise Realtime Messaging, WebSocket Scale-out, and WebRTC Signaling Lab</h3>

<p>
  <img src="https://img.shields.io/badge/Java-21_LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-4.0.6-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white" />
  <img src="https://img.shields.io/badge/Redis-Pub%2FSub-DC382D?style=for-the-badge&logo=redis&logoColor=white" />
  <img src="https://img.shields.io/badge/WebRTC-P2P_Signaling-333333?style=for-the-badge&logo=webrtc&logoColor=white" />
</p>

<p>
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" />
  <img src="https://img.shields.io/badge/Coverage-93%25-brightgreen?style=flat-square" />
  <img src="https://img.shields.io/badge/Build-Passing-brightgreen?style=flat-square&logo=githubactions&logoColor=white" />
</p>

</div>

---

> WebSocket scale-out, STOMP handshake security, WebRTC signaling을 하나의 실시간 통신 아키텍처로 검증하는 엔터프라이즈 레퍼런스입니다.  
> 단일 서버 채팅 예제를 넘어, 다중 노드 환경의 메시지 동기화와 P2P 미디어 연결 수립 흐름을 코드와 테스트로 증명합니다.

---

## 📌 Problem — 왜 만들었는가

- **WebSocket scale-out 한계**: 단일 서버 메모리 기반 메시징은 서버가 늘어날수록 세션과 메시지 동기화 문제가 발생합니다.
- **실시간 연결 인증 복잡성**: WebSocket/STOMP 연결은 일반 HTTP 요청과 다르게 별도 handshake 인증 전략이 필요합니다.
- **WebRTC 서버 부하 문제**: 화상/음성 미디어 트래픽을 서버가 직접 중계하면 비용과 지연 시간이 커집니다.
- **시간 데이터 신뢰성**: 클라이언트가 보낸 timestamp를 그대로 믿으면 메시지 정렬과 감사 로그가 왜곡될 수 있습니다.

Realtime Comm Lab은 Redis Pub/Sub 기반 분산 채팅, JWT STOMP handshake interceptor, WebRTC P2P signaling을 통해 실시간 시스템의 확장성과 연결 보안을 함께 다룹니다.

## 🏗️ Architecture — 어떻게 설계했는가

### STOMP & Redis Pub/Sub Architecture

```mermaid
sequenceDiagram
    autonumber
    actor C1 as Client A
    participant N1 as Node 1
    participant Redis as Redis Pub/Sub
    participant N2 as Node 2
    actor C2 as Client B

    C1->>N1: CONNECT /ws-chat with JWT
    C2->>N2: CONNECT /ws-chat with JWT
    C1->>N1: SEND /app/chat/message
    N1->>Redis: PUBLISH message to room topic
    Redis-->>N2: SUBSCRIBE broadcast message
    N2->>C2: MESSAGE /topic/chat/room/{id}
```

### WebRTC P2P Signaling Architecture

```mermaid
sequenceDiagram
    autonumber
    actor PeerA as WebRTC Peer A
    participant SigServer as Signaling Server
    actor PeerB as WebRTC Peer B

    PeerA->>SigServer: SDP Offer
    SigServer->>PeerB: Broadcast Offer
    PeerB->>SigServer: SDP Answer
    SigServer->>PeerA: Broadcast Answer
    PeerA->>SigServer: ICE Candidate
    SigServer->>PeerB: Broadcast Candidate
    PeerA->>PeerB: Direct P2P Media Stream
```

## 📂 Project Structure

```text
realtime-comm-lab/
├── .github/workflows/                         # ⚙️ CI/CD 자동화 파이프라인
├── src/main/java/com/hooney/lab/realtime/
│   ├── chat/                                  # 💬 STOMP 기반 분산 채팅 도메인
│   │   ├── controller/                        # 🌐 메시지 라우팅 및 서버 timestamp 보정
│   │   ├── dto/                               # 📦 채팅 요청/응답 메시지 모델
│   │   └── pubsub/                            # 📡 Redis Publisher/Subscriber 연동
│   ├── config/                                # ⚙️ WebSocket, STOMP, Redis 설정
│   ├── security/                              # 🛡️ JWT handshake 인증 인터셉터
│   └── webrtc/                                # 🎥 P2P 화상 통신 signaling 도메인
├── src/test/java/com/hooney/lab/realtime/
│   ├── chat/                                  # 🧪 Embedded Redis 기반 pub/sub 통합 테스트
│   └── security/                              # 🧪 STOMP CONNECT 인증 실패/성공 테스트
├── build.gradle                               # 🧰 Spring Boot, Redis, WebSocket 의존성
├── docker-compose.yml                         # 🐳 Redis + 다중 App Node scale-out 시뮬레이션
└── Dockerfile                                 # 📦 컨테이너 이미지 빌드 정의
```

## 🎯 Key Features & Evidence — 무엇을 증명하는가

### 1. Redis Pub/Sub 기반 Scale-out Messaging

| Feature | Description |
| :--- | :--- |
| **Multi-node Broadcast** | 여러 애플리케이션 노드가 Redis topic을 통해 같은 채팅방 메시지를 공유 |
| **Server-side Timestamp** | 클라이언트 시간 위조를 방지하기 위해 서버 인입 시점 기준으로 시간 보정 |
| **Embedded Redis Test** | 로컬 Redis 없이도 분산 메시징 흐름을 검증 |

**Evidence**

- `ChatMessagePublisher`와 `RedisMessageSubscriber`로 노드 간 메시지 전파를 구현합니다.
- Embedded Redis 기반 통합 테스트로 publish/subscribe 흐름을 검증합니다.
- 단일 `SimpleBroker` 구조에서 발생하는 노드 간 메시지 불일치 문제를 Redis broker로 해결합니다.

### 2. WebRTC P2P Signaling Server

| Signal | Role |
| :--- | :--- |
| **OFFER** | 연결을 시작하는 Peer의 미디어 세션 제안 |
| **ANSWER** | 상대 Peer의 미디어 세션 응답 |
| **ICE Candidate** | 네트워크 경로 탐색을 위한 후보 정보 교환 |

**Evidence**

- `WebRTCSignalingController`가 미디어 트래픽을 직접 처리하지 않고 signaling 정보만 중계합니다.
- 연결 수립 후 미디어는 Peer 간 직접 흐르도록 하여 서버 부하를 줄입니다.

### 3. JWT Handshake Interceptor

| Risk | Strategy | Evidence |
| :--- | :--- | :--- |
| 비인가 WebSocket 연결 | STOMP CONNECT 단계에서 JWT 검증 | `JwtChannelInterceptor` |
| HTTP 필터 우회 | WebSocket 전용 인증 흐름 분리 | Interceptor unit test |
| 리소스 선점 공격 | 연결 초기에 인증 실패 처리 | Handshake failure scenario |

**Evidence**

- 최초 연결 시점에 JWT를 검증하여 비인가 클라이언트의 세션 생성을 차단합니다.
- HTTP API 인증과 WebSocket 인증을 분리해 프로토콜 차이를 명확히 처리합니다.

## 🚀 Quick Start — 어떻게 실행하는가

### 로컬 개발 환경

```bash
git clone https://github.com/hooneyg/realtime-comm-lab.git
cd realtime-comm-lab

./gradlew test
./gradlew bootRun
```

### Docker Multi-node 시뮬레이션

```bash
docker-compose up -d --build
docker-compose logs -f
```

---

## ⚡ 분산 웹소켓 실시간 테스터 대시보드 (WebSocket Live Tester)

본 프로젝트는 분산 환경의 웹소켓 동작 및 WebRTC 시그널링을 손쉽게 검증할 수 있도록 스프링 시큐리티 우회 필터를 적용한 **Glassmorphism 스타일의 프리미엄 실시간 테스터 대시보드**를 내장하고 있습니다.

### 🌐 접속 주소
- **Application Node 1**: [http://localhost:8081/ws-tester.html](http://localhost:8081/ws-tester.html)
- **Application Node 2**: [http://localhost:8082/ws-tester.html](http://localhost:8082/ws-tester.html)

### ✨ 주요 핵심 기능
- **원클릭 가짜 JWT 생성기:** STOMP Handshake 시 필수 항목인 JWT 인증 필터를 위해, 사용자 ID만 입력하면 즉시 1시간짜리 테스트용 JWT 토큰을 자동 발행 및 주입합니다.
- **다이나믹 드롭다운 템플릿:** 일반 대화(`TALK`), 채팅방 입장(`ENTER`), WebRTC 룸 조인(`JOIN`), WebRTC SDP 정보(`OFFER`/`ANSWER`), ICE 후보(`ICE`) 등 다양한 시나리오 페이로드 구조를 마우스 원클릭으로 주입하고 즉시 포맷팅해 줍니다.
- **실시간 Fira Code 터미널:** 유입되는 모든 인바운드(`INBOUND`), 아웃바운드(`OUTBOUND`) 프레임을 미려하게 가공하고 실시간 시간과 태그별 컬러 하이라이팅을 입혀 가시성을 대폭 향상했습니다.

### 🧪 포트폴리오용 다중 노드(Node-1 ↔ Node-2) 동기화 검증 시나리오
> Redis Pub/Sub을 거쳐 다중 노드 간 실시간 메시지가 완벽하게 동화되는 분산 스케일아웃 기술의 핵심 증적 자료입니다.

1. **브라우저에 두 개의 탭(또는 창)을 나란히 배치**합니다.
   - **탭 A**: [http://localhost:8081/ws-tester.html](http://localhost:8081/ws-tester.html) (Node 1)
   - **탭 B**: [http://localhost:8082/ws-tester.html](http://localhost:8082/ws-tester.html) (Node 2)
2. **탭 A (Node 1)**:
   - 임시 사용자 식별자에 `user-A` 입력 후 `JWT 토큰 생성` 클릭
   - 초록색 `Connect` 버튼 클릭 (연결 상태가 `Connected`로 변경됨)
   - 핑크색 `구독 실행` 버튼 클릭 (토픽 경로 `/topic/chat/room/room-777` 구독 개시)
3. **탭 B (Node 2)**:
   - 임시 사용자 식별자에 `user-B` 입력 후 `JWT 토큰 생성` 클릭
   - 초록색 `Connect` 버튼 클릭 및 `/topic/chat/room/room-777` 경로 동일하게 `구독 실행`
4. **메시지 전송 및 동기화 확인**:
    - **탭 A**에서 `STOMP 메시지 발행하기 (Send Frame)`를 클릭합니다.
    - **탭 B (Node 2)**의 터미널 콘솔 로그 창을 확인하면, 서로 다른 포트/애플리케이션 노드에 붙어있음에도 불구하고 **Redis Pub/Sub을 타고 탭 A가 발행한 메시지가 탭 B로 실시간 수신(INBOUND)되는 놀라운 현상**을 눈으로 직접 검증할 수 있습니다!

### 📡 Redis Pub/Sub 실시간 메시지 흐름 관측 기법 (중요!)
> ⚠️ **아키텍처적 주의 사항**: Redis Pub/Sub은 **"발행 및 망각(Fire and Forget)"** 방식의 초경량 실시간 푸시 엔진입니다. 메시지가 메모리나 디스크에 Key-Value 형태로 저장되지 않고, 채널을 리스닝 중인 구독자에게 밀어준 뒤 **즉시 소멸**합니다. 레디스 내부를 관통하는 실시간 메시지 흐름을 확인하고 싶을 때, 다음 두 가지 모니터링 수단을 즉시 활용할 수 있습니다.


#### 1. CLI 터미널 실시간 스트리밍 관측 (강력 추천 ⭐)
새로운 명령 프롬프트나 터미널을 열고 아래 명령어를 입력하여 Redis 내부의 모든 메시지 발행(Publish) 이벤트를 실시간으로 가로채어 모니터링합니다.
```bash
# Redis CLI의 패턴 구독(psubscribe) 명령을 이용해 흐르는 모든 메시지 덤프
docker exec -it rtc-redis redis-cli -a rtc-secret psubscribe "*"
```
*메시지가 전송될 때마다 터미널에 메시지 본문과 채널명이 실시간으로 스트리밍 출력됩니다.*

#### 2. Redis Commander Web GUI 모니터링 (Port: 8085)
도커 환경에 경량화된 Redis Web UI 도구인 **`Redis Commander`**를 함께 구성해 두었습니다.
- **접속 주소:** [http://localhost:8085](http://localhost:8085)
- **활용법:** 왼쪽 트리에서 `local` Redis 인스턴스 정보와 시스템 지표를 확인하거나, 우측 상단의 `CLI` 탭을 열고 `psubscribe *` 또는 `monitor` 명령어를 입력하여 브라우저에서 편리하게 Redis 실시간 흐름을 조회할 수 있습니다.



## 🧪 Tests — 어떻게 검증했는가

```bash
./gradlew test
```

| Test Target | What It Proves |
| :--- | :--- |
| Redis Pub/Sub integration | 노드 간 메시지 전파와 구독 흐름 |
| WebRTC signaling controller | Offer, Answer, ICE Candidate 라우팅 |
| JWT channel interceptor | 토큰 누락, 만료, 위조 연결 차단 |
| GitHub Actions CI | push마다 테스트와 빌드 자동 검증 |

## 🧭 Roadmap

- [ ] WebSocket session registry 고도화
- [ ] Redis Streams 적용 검토
- [ ] WebRTC room/session lifecycle 관리
- [ ] Connection metrics 수집
- [ ] Load test 시나리오 추가

## 🔗 Related Labs

| Related Lab | 연결 이유 |
| :--- | :--- |
| `security-auth-core` | WebSocket과 signaling 요청의 인증/인가 기준 |
| `infra-master-lab` | 다중 노드 실시간 서버 배포와 운영 기준 |
| `database-master-lab` | 채팅방, 메시지, 세션 상태 저장 기준 |
| `event-streaming-lab` | 실시간 이벤트와 비동기 메시지 처리 기준 |
| `ai-agent-brain-lab` | 사용자와 AI Agent 간 실시간 대화 채널 확장 |

## 📚 Documentation

- [WebSocket Scaling Strategy](./docs/websocket-scaling.md)
- [Redis Pub/Sub Clustering](./docs/redis-pubsub-clustering.md)
- [WebRTC Signaling Flow](./docs/webrtc-signaling-flow.md)
- [JWT Handshake Auth](./docs/jwt-handshake-auth.md)
- [Troubleshooting Guide](./docs/troubleshooting.md)

## 📄 License

This project is licensed under the [MIT License](./LICENSE).

---

<div align="center">
<b>Built by <a href="https://github.com/hooneyg">Hooney</a> — AI FullStack Developer & Enterprise Solution Architect</b>

<img src="https://capsule-render.vercel.app/api?type=waving&color=FF8C00&height=100&section=footer" />
</div>
