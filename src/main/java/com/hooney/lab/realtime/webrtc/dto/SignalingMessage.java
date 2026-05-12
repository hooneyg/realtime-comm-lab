package com.hooney.lab.realtime.webrtc.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * WebRTC P2P 연결을 위한 시그널링 메시지 포맷 (DTO)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignalingMessage {

    /**
     * WebRTC 시그널링 타입
     * - OFFER: P2P 연결을 제안하는 세션 정보 (SDP)
     * - ANSWER: 제안에 대한 수락 및 자신의 세션 정보 (SDP)
     * - ICE: 서로 통신 가능한 네트워크 경로(IP, Port) 후보군 교환
     * - JOIN: 화상 채팅방 입장 알림
     * - LEAVE: 화상 채팅방 퇴장 알림
     */
    public enum SignalingType {
        OFFER, ANSWER, ICE, JOIN, LEAVE
    }

    private SignalingType type;    // 시그널링 타입
    private String roomId;         // 화상 통화방 ID
    private String sender;         // 보내는 사람의 식별자(Session ID 또는 User ID)
    private Object data;           // SDP 정보 문자열 또는 ICE Candidate JSON 객체
}
