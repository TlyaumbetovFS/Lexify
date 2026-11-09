package com.lexify.service;

import com.lexify.dto.ChatEvent;
import com.lexify.dto.MessageResponse;
import java.time.Instant;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.*;

class ChatEventServiceTest {

    private final SimpMessagingTemplate messaging = mock(SimpMessagingTemplate.class);
    private final ChatEventService service = new ChatEventService(messaging);

    @ParameterizedTest
    @ValueSource(strings = {"ROOM_CREATED", "ROOM_UPDATED", "ROOM_DELETED"})
    void publishesRoomChangesOnlyToRoomList(String type) {
        var event = new ChatEvent(type, 1L, null);

        service.publish(event);

        verify(messaging).convertAndSend("/topic/rooms", event);
        verifyNoMoreInteractions(messaging);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MESSAGE_CREATED", "MESSAGE_UPDATED", "MESSAGE_DELETED"})
    void publishesMessagesOnlyToTheirRoom(String type) {
        var message = new MessageResponse(2L, 1L, 3L, "hello", Instant.now());
        var event = new ChatEvent(type, 1L, message);

        service.publish(event);

        verify(messaging).convertAndSend("/topic/rooms/1", event);
        verifyNoMoreInteractions(messaging);
    }
}
