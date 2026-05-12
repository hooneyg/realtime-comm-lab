package com.hooney.lab.realtime.chat;

import com.hooney.lab.realtime.chat.controller.ChatController;
import com.hooney.lab.realtime.chat.dto.ChatMessage;
import com.hooney.lab.realtime.chat.pubsub.ChatMessagePublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * ChatController 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatMessagePublisher publisher;

    @InjectMocks
    private ChatController chatController;

    @Test
    @DisplayName("입장(ENTER) 메시지 수신 시 시스템 메시지로 변환하여 Publish 한다")
    void handleChatMessage_Enter() {
        // given
        ChatMessage message = ChatMessage.builder()
                .type(ChatMessage.MessageType.ENTER)
                .roomId("room1")
                .sender("hooney")
                .build();

        // when
        chatController.handleChatMessage(message);

        // then
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(publisher).publish(captor.capture());

        ChatMessage publishedMsg = captor.getValue();
        assertThat(publishedMsg.getSender()).isEqualTo("[System]");
        assertThat(publishedMsg.getMessage()).contains("hooney님이 방에 입장하셨습니다.");
    }

    @Test
    @DisplayName("일반 채팅(TALK) 메시지 수신 시 타임스탬프를 보정하여 Publish 한다")
    void handleChatMessage_Talk() {
        // given
        ChatMessage message = ChatMessage.builder()
                .type(ChatMessage.MessageType.TALK)
                .roomId("room1")
                .sender("hooney")
                .message("Hello")
                .timestamp(0) // 과거의 조작된 시간
                .build();

        // when
        chatController.handleChatMessage(message);

        // then
        ArgumentCaptor<ChatMessage> captor = ArgumentCaptor.forClass(ChatMessage.class);
        verify(publisher).publish(captor.capture());

        ChatMessage publishedMsg = captor.getValue();
        assertThat(publishedMsg.getSender()).isEqualTo("hooney");
        assertThat(publishedMsg.getMessage()).isEqualTo("Hello");
        assertThat(publishedMsg.getTimestamp()).isGreaterThan(0); // 현재 시간으로 보정됨을 확인
    }
}
