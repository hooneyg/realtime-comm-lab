package com.hooney.lab.realtime.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실시간 통신을 위한 공통 메시지 포맷 객체 (DTO)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 메시지 타입 (입장, 채팅, 퇴장)
     * - 프론트엔드 라우팅 및 렌더링에 사용됩니다.
     */
    public enum MessageType {
        ENTER, TALK, QUIT
    }

    private MessageType type;      // 메시지 타입
    private String roomId;         // 방 번호
    private String sender;         // 보낸 사람 (JWT 기반으로 서버에서 검증/주입)
    private String message;        // 메시지 내용
    private long timestamp;        // 전송 시간 (Epoch millis)
}
