package com.hooney.lab.realtime.webrtc.controller;

import com.hooney.lab.realtime.webrtc.dto.SignalingMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;

/**
 * WebRTC 화상 통화 P2P 연결 수립을 돕는 시그널링(Signaling) 컨트롤러
 * 
 * [Architecture Note]
 * WebRTC는 클라이언트 간(P2P) 직접 미디어(영상/음성) 데이터를 주고받습니다.
 * 하지만 P2P 연결이 성립되려면 서로의 IP 주소와 미디어 코덱 정보(SDP)를 알아야 합니다.
 * 이 컨트롤러는 클라이언트들이 STOMP 웹소켓 망을 통해 서로의 연결 정보를 
 * 안전하고 빠르게 교환(중계)할 수 있도록 "전화 교환기" 역할을 수행합니다.
 * 일단 연결이 수립(Connected)되면 서버의 부하 없이 클라이언트끼리 통신합니다!
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class WebRTCSignalingController {

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 클라이언트가 `/app/rtc/signal` 경로로 시그널링 메시지를 보내면,
     * 해당 방에 있는 다른 클라이언트들에게 브로드캐스트하여 정보를 교환합니다.
     */
    @MessageMapping("/rtc/signal")
    public void processSignalingMessage(SignalingMessage message) {
        log.info("WebRTC 시그널링 수신 [Room: {}] Type: {}, Sender: {}", 
                message.getRoomId(), message.getType(), message.getSender());

        // 시그널링 데이터(SDP, ICE)는 가공 없이 해당 방의 구독자들에게 그대로 패스(Bypass)
        messagingTemplate.convertAndSend("/topic/rtc/room/" + message.getRoomId(), message);
    }
}
