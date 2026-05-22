# 🎥 WebRTC Signaling Flow (WebRTC 시그널링 흐름)

이 문서는 `realtime-comm-lab`에서 WebRTC(Web Real-Time Communication) 기술을 활용하여 Peer-to-Peer(P2P) 실시간 미디어 스트림(화상, 음성, 데이터)을 직접 교환하기 전에 수행하는 **시그널링(Signaling) 프로토콜**의 연결 수립 프로세스를 설명합니다.

---

## 📌 1. WebRTC 시그널링의 필요성 (Why Signaling?)

WebRTC는 궁극적으로 두 클라이언트가 직접 미디어 데이터를 전송(P2P)하는 기술입니다. 하지만 방화벽(Firewall), NAT(Network Address Translation), 공인 IP 미인지 등으로 인해 두 브라우저는 서로의 존재와 위치를 직접 알아낼 수 없습니다. 

따라서 중간에서 각 브라우저의 네트워크 연결 후보와 미디어 호환성 정보를 안전하게 릴레이해 주는 **시그널링 서버(Signaling Server)**가 필수적으로 요구됩니다. 시그널링 단계에서 주고받는 정보는 크게 두 가지입니다:
1. **SDP(Session Description Protocol)**: 비디오/오디오 코덱 설정, 해상도 등 미디어 세션에 대한 정보 (Offer / Answer)
2. **ICE Candidate**: NAT/방화벽 통과를 위한 네트워크 연결 경로 후보들

---

## 🏗️ 2. 시그널링 서버 중계 시퀀스 (Sequence Diagram)

본 랩(Lab)에서는 **Spring Boot + STOMP WebSocket**을 시그널링 프로토콜의 중계 통로로 활용합니다. 아래는 두 Peer(A, B) 간의 연결 수립 시퀀스입니다.

```mermaid
sequenceDiagram
    autonumber
    actor PeerA as WebRTC Peer A
    participant SigServer as Signaling Server (Spring Boot)
    actor PeerB as WebRTC Peer B

    Note over PeerA, PeerB: 1. WebSocket / STOMP 연결 수립 (JWT 인증)
    PeerA->>SigServer: CONNECT /ws-chat (with JWT)
    PeerB->>SigServer: CONNECT /ws-chat (with JWT)

    Note over PeerA, PeerB: 2. SDP Offer 전송
    PeerA->>SigServer: SEND /app/webrtc/offer (Payload: SDP)
    SigServer->>PeerB: SUBSCRIBE /topic/webrtc/room/777 (Receive Offer)

    Note over PeerB, PeerA: 3. SDP Answer 응답
    PeerB->>SigServer: SEND /app/webrtc/answer (Payload: SDP)
    SigServer->>PeerA: SUBSCRIBE /topic/webrtc/room/777 (Receive Answer)

    Note over PeerA, PeerB: 4. ICE Candidates 교환 (비동기 병렬)
    par Peer A to B
        PeerA->>SigServer: SEND /app/webrtc/candidate
        SigServer->>PeerB: Receive Candidate
    and Peer B to A
        PeerB->>SigServer: SEND /app/webrtc/candidate
        SigServer->>PeerA: Receive Candidate
    end

    Note over PeerA, PeerB: 5. NAT 홀펀칭 및 P2P 직접 미디어 스트림 연결
    PeerA<->>PeerB: Direct WebRTC P2P Media Stream Connection (STUN/TURN)
```

---

## 🎯 3. 주요 구성 요소 사양 (Core Terms)

- **SDP Offer (제안)**: Peer A가 자신의 미디어 및 네트워크 설정을 정리하여 시그널링 서버를 통해 Peer B에게 전달하는 첫 번째 세션 메타데이터입니다.
- **SDP Answer (응답)**: Peer B가 Peer A의 Offer를 확인한 뒤, 자신과 호환되는 미디어 정보를 매핑하여 Peer A에게 다시 돌려주는 세션 정보입니다.
- **ICE (Interactive Connectivity Establishment)**: 두 Peer 간 최적의 통신 경로를 찾는 프레임워크입니다.
  - **STUN Server**: 자신의 공인 IP와 포트 번호를 파악하기 위한 경량 서버입니다.
  - **TURN Server**: 대칭 NAT(Symmetric NAT) 등으로 인해 P2P 홀펀칭이 완전히 불가능할 때, 트래픽을 임시로 프록시해주는 릴레이 서버입니다.
- **Spring Boot Controller**: `/app/webrtc/*`로 유입되는 신호를 `@MessageMapping` 어노테이션으로 매핑하여 수신처를 제외한 대상 채팅방/통화방 전체에 브로드캐스트합니다. 시그널링 서버 자체는 대용량 미디어 데이터를 처리하지 않으므로 CPU 및 네트워크 비용이 매우 절감됩니다.
