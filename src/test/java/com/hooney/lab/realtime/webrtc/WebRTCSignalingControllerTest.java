package com.hooney.lab.realtime.webrtc;

import com.hooney.lab.realtime.webrtc.controller.WebRTCSignalingController;
import com.hooney.lab.realtime.webrtc.dto.SignalingMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * WebRTCSignalingController 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class WebRTCSignalingControllerTest {

    @Mock
    private SimpMessageSendingOperations messagingTemplate;

    @InjectMocks
    private WebRTCSignalingController signalingController;

    @Test
    @DisplayName("시그널링 메시지를 수신하면 해당 화상 방(/topic/rtc/room/{roomId})으로 브로드캐스트한다")
    void processSignalingMessage() {
        // given
        String roomId = "rtc-room-1";
        SignalingMessage sdpOffer = SignalingMessage.builder()
                .type(SignalingMessage.SignalingType.OFFER)
                .roomId(roomId)
                .sender("userA")
                .data("v=0\\r\\no=- 4611731400430051336 2 IN IP4 127.0.0.1\\r\\n")
                .build();

        // when
        signalingController.processSignalingMessage(sdpOffer);

        // then
        // 가공 없이 그대로 해당 방으로 던져지는지 검증
        verify(messagingTemplate).convertAndSend(eq("/topic/rtc/room/" + roomId), eq(sdpOffer));
    }
}
