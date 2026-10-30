package com.lexify.service;

import com.lexify.dto.ChatEvent;
import com.lexify.dto.MessageRequest;
import com.lexify.dto.MessageResponse;
import com.lexify.mapper.MessageMapper;
import com.lexify.model.ChatMessage;
import com.lexify.model.ChatRoom;
import com.lexify.model.User;
import com.lexify.repository.MessageRepository;
import com.lexify.repository.RoomRepository;
import com.lexify.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messages;
    @Mock
    private RoomRepository rooms;
    @Mock
    private UserRepository users;
    @Mock
    private ApplicationEventPublisher events;
    private MessageService service;

    @BeforeEach
    void setUp() {
        service = new MessageService(messages, rooms, users, Mappers.getMapper(MessageMapper.class), events);
    }

    @Test
    void savesAuthorTimestampAndTrimmedTextBeforePublishingEvent() {
        when(rooms.findById(10L)).thenReturn(Optional.of(room()));
        when(users.getReferenceById(1L)).thenReturn(user(1L));
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> {
            var message = invocation.getArgument(0, ChatMessage.class);
            message.setId(20L);
            return message;
        });
        var before = Instant.now();

        var result = service.create(1L, 10L, new MessageRequest(" Hello! "));

        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.roomId()).isEqualTo(10L);
        assertThat(result.text()).isEqualTo("Hello!");
        assertThat(result.createdAt()).isBetween(before, Instant.now());
        var order = inOrder(messages, events);
        order.verify(messages).saveAndFlush(any());
        order.verify(events).publishEvent(new ChatEvent("MESSAGE_CREATED", 10L, result));
    }

    @Test
    void authorCanEditWithoutChangingAuthorOrCreationTime() {
        var message = message();
        when(messages.findByIdAndRoomId(20L, 10L)).thenReturn(Optional.of(message));
        stubSave();

        var result = service.update(1L, 10L, 20L, new MessageRequest("Updated"));

        assertThat(result.text()).isEqualTo("Updated");
        assertThat(result.authorId()).isEqualTo(1L);
        assertThat(result.createdAt()).isEqualTo(Instant.parse("2026-10-30T00:00:00Z"));
        verify(events).publishEvent(new ChatEvent("MESSAGE_UPDATED", 10L, result));
    }

    @Test
    void nonAuthorCannotEditEvenIfModerator() {
        when(messages.findByIdAndRoomId(20L, 10L)).thenReturn(Optional.of(message()));
        assertStatus(403, () -> service.update(2L, 10L, 20L, new MessageRequest("Changed")));
        verify(messages, never()).saveAndFlush(any());
        verifyNoInteractions(events, rooms);
    }

    @Test
    void authorCanDeleteOwnMessage() {
        var message = message();
        when(messages.findByIdAndRoomId(20L, 10L)).thenReturn(Optional.of(message));

        service.delete(1L, 10L, 20L);

        verify(messages).delete(message);
        verifyNoInteractions(rooms);
        verify(events).publishEvent(new ChatEvent("MESSAGE_DELETED", 10L,
                new MessageResponse(20L, 10L, 1L, "Hello", message.getCreatedAt())));
    }

    @Test
    void roomModeratorCanDeleteAnotherAuthorsMessage() {
        var message = message();
        when(messages.findByIdAndRoomId(20L, 10L)).thenReturn(Optional.of(message));
        when(rooms.existsByIdAndModeratorsId(10L, 2L)).thenReturn(true);

        service.delete(2L, 10L, 20L);

        verify(messages).delete(message);
        verify(events).publishEvent(any(ChatEvent.class));
    }

    @Test
    void ordinaryUserOrModeratorFromOtherRoomCannotDeleteMessage() {
        when(messages.findByIdAndRoomId(20L, 10L)).thenReturn(Optional.of(message()));
        assertStatus(403, () -> service.delete(2L, 10L, 20L));
        verify(rooms).existsByIdAndModeratorsId(10L, 2L);
        verify(messages, never()).delete(any());
        verifyNoInteractions(events);
    }

    @Test
    void historyUsesRequestedPageAndMapsMessages() {
        when(rooms.existsById(10L)).thenReturn(true);
        when(messages.findByRoomIdOrderByIdDesc(10L, PageRequest.of(2, 20))).thenReturn(List.of(message()));
        assertThat(service.list(10L, 2, 20)).extracting(MessageResponse::id).containsExactly(20L);
    }

    @Test
    void cannotReadMessageThroughDifferentRoom() {
        assertStatus(404, () -> service.get(11L, 20L));
        verify(messages).findByIdAndRoomId(20L, 11L);
    }

    @Test
    void cannotCreateMessageInMissingRoom() {
        assertStatus(404, () -> service.create(1L, 999L, new MessageRequest("Hello")));
        verify(messages, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    @Test
    void missingRoomHistoryReturnsNotFound() {
        assertStatus(404, () -> service.list(999L, 0, 50));
        verifyNoInteractions(messages);
    }

    @Test
    void rejectsBlankTextWithoutSavingOrPublishing() {
        when(rooms.findById(10L)).thenReturn(Optional.of(room()));
        assertStatus(400, () -> service.create(1L, 10L, new MessageRequest(" 	 ")));
        verify(messages, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    private void stubSave() {
        when(messages.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ChatMessage message() {
        var message = new ChatMessage();
        message.setId(20L);
        message.setRoom(room());
        message.setAuthor(user(1L));
        message.setText("Hello");
        message.setCreatedAt(Instant.parse("2026-10-30T00:00:00Z"));
        return message;
    }

    private ChatRoom room() {
        var room = new ChatRoom();
        room.setId(10L);
        return room;
    }

    private User user(Long id) {
        var user = new User();
        user.setId(id);
        return user;
    }

    private void assertStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                exception -> assertThat(exception.getStatusCode().value()).isEqualTo(status));
    }
}
