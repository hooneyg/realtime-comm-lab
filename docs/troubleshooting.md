# 🛠️ Realtime Comm Lab Troubleshooting Guide

본 문서는 `realtime-comm-lab` 프로젝트의 핵심 컴포넌트 개발 및 운영 과정에서 마주칠 수 있는 주요 트러블슈팅 사례와 해결 과정을 기록합니다.

---

## 1. Redis Pub/Sub 직렬화(Serialization) 오류

### 🚨 Problem (증상)
분산 환경 시뮬레이션을 위해 `docker-compose`로 노드를 띄우고 채팅을 보냈을 때,
Subscribing 노드에서 다음과 같은 예외가 발생하며 클라이언트에게 메시지가 도달하지 못함.

```text
org.springframework.data.redis.serializer.SerializationException: Could not read JSON: Cannot construct instance of `com.hooney.lab.realtime.chat.dto.ChatMessage` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)
```

### 🔍 Cause Analysis (원인 분석)
- `ChatMessage` DTO 객체가 Redis로 들어갈 때 바이트 배열로 직렬화되고, 구독하는 쪽에서 다시 객체로 역직렬화(Deserialization)되어야 함.
- Jackson 라이브러리가 역직렬화를 수행할 때 기본 생성자(Default Constructor)를 필요로 하는데, `ChatMessage`에 `@Builder`나 파라미터가 있는 생성자만 존재하고 기본 생성자가 누락되었음.

### ✅ Solution (해결 방안)
DTO 클래스에 `@NoArgsConstructor`를 추가하여 Jackson이 객체를 인스턴스화할 수 있도록 조치.

```java
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor // 역직렬화를 위한 필수 어노테이션 추가
public class ChatMessage {
    private MessageType type;
    private String roomId;
    private String sender;
    private String message;
    private long timestamp;
    
    // ...
}
```

---

## 2. STOMP 연결 시 JWT 검증 실패 및 커넥션 끊김 현상

### 🚨 Problem (증상)
클라이언트가 `ws://localhost:8080/ws-chat`으로 접속 시도 시 브라우저 콘솔에서 즉시 연결이 강제 종료되며 서버 로그에 `Missing Authorization header` 에러가 발생.

### 🔍 Cause Analysis (원인 분석)
- `JwtChannelInterceptor`가 작동 중이며, `StompCommand.CONNECT` 단계에서 Authorization 헤더를 필수로 요구함.
- 브라우저 기본 WebSocket API나 단순 SockJS 구현체에서는 헤더를 자동으로 심어주지 않음.
- STOMP.js 설정 시 `connectHeaders` 객체에 토큰을 명시적으로 넣어주어야 백엔드의 Interceptor가 값을 추출할 수 있음.

### ✅ Solution (해결 방안)
프론트엔드 연결부 코드 점검 및 가이드 문서화. 백엔드 로직 자체는 "Fail-fast" 방어를 훌륭하게 수행하고 있는 상태이므로 클라이언트 구현부만 수정.

```javascript
// 프론트엔드 연결 스니펫 (예시)
const client = new StompJs.Client({
    brokerURL: 'ws://localhost:8080/ws-chat',
    connectHeaders: {
        Authorization: 'Bearer ' + myJwtToken, // JWT 주입
    },
    // ...
});
```

---

## 3. WebRTC P2P ICE Candidate 지연 및 통신 실패

### 🚨 Problem (증상)
방화벽 내부에 있는 두 클라이언트가 STOMP 서버를 통해 시그널링 데이터(Offer, Answer)를 성공적으로 교환했음에도, 
WebRTC P2P 연결(ICE Gathering 단계)이 완료되지 않고 통신이 실패함.

### 🔍 Cause Analysis (원인 분석)
- 로컬 망(동일한 서브넷)에서는 STUN 서버 없이도 P2P 연결이 될 수 있으나, 서로 다른 NAT 장비 뒤에 있거나 엄격한 방화벽(Symmetric NAT) 뒤에 있는 클라이언트 간에는 Public IP를 알 수 없음.
- 프로젝트 코드 내 STUN/TURN 서버 설정 누락으로 인해 릴레이 통신 경로를 확보하지 못함.

### ✅ Solution (해결 방안)
클라이언트 측 `RTCPeerConnection` 설정 시 Google의 퍼블릭 STUN 서버를 기본값으로 지정하고, 필요시 TURN 서버(예: Twilio, Coturn)를 연동하도록 가이드라인 추가. (시그널링 서버의 코드는 변경 없음)

```javascript
const configuration = {
    'iceServers': [
        {'urls': 'stun:stun.l.google.com:19302'} // 공용 STUN 서버 적용
    ]
};
const peerConnection = new RTCPeerConnection(configuration);
```
