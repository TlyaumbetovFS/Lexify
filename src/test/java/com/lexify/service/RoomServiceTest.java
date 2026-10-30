package com.lexify.service;

import com.lexify.dto.ChatEvent;
import com.lexify.dto.RoomRequest;
import com.lexify.mapper.RoomMapper;
import com.lexify.model.ChatRoom;
import com.lexify.model.User;
import com.lexify.repository.RoomRepository;
import com.lexify.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository rooms;
    @Mock
    private UserRepository users;
    @Mock
    private ApplicationEventPublisher events;
    private RoomService service;

    @BeforeEach
    void setUp() {
        service = new RoomService(rooms, users, Mappers.getMapper(RoomMapper.class), events);
    }

    @Test
    void creatorBecomesOwnerAndModerator() {
        when(users.getReferenceById(1L)).thenReturn(user(1L));
        when(rooms.saveAndFlush(any())).thenAnswer(invocation -> {
            var room = invocation.getArgument(0, ChatRoom.class);
            room.setId(10L);
            return room;
        });

        var result = service.create(1L, new RoomRequest(" Travel ", "Trips"));

        assertThat(result.ownerId()).isEqualTo(1L);
        assertThat(result.moderatorIds()).containsExactly(1L);
        assertThat(result.name()).isEqualTo("Travel");
        verify(events).publishEvent(new ChatEvent("ROOM_CREATED", 10L, null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"update", "delete", "addModerator", "removeModerator"})
    void onlyOwnerCanManageRoom(String action) {
        when(rooms.findById(10L)).thenReturn(Optional.of(room()));

        assertStatus(403, () -> {
            switch (action) {
                case "update" -> service.update(2L, 10L, new RoomRequest("Changed", null));
                case "delete" -> service.delete(2L, 10L);
                case "addModerator" -> service.addModerator(2L, 10L, 3L);
                case "removeModerator" -> service.removeModerator(2L, 10L, 1L);
                default -> throw new IllegalArgumentException(action);
            }
        });

        verify(rooms, never()).saveAndFlush(any());
        verify(rooms, never()).delete(any());
        verifyNoInteractions(events, users);
    }

    @Test
    void ownerCanRenameRoomAndClearDescription() {
        var room = room();
        room.setDescription("Old");
        when(rooms.findById(10L)).thenReturn(Optional.of(room));
        stubSave();

        var result = service.update(1L, 10L, new RoomRequest(" New ", null));

        assertThat(result.name()).isEqualTo("New");
        assertThat(result.description()).isNull();
        assertThat(result.moderatorIds()).containsExactly(1L);
        verify(events).publishEvent(new ChatEvent("ROOM_UPDATED", 10L, null));
    }

    @Test
    void ownerCanAddModeratorWithoutDuplicates() {
        when(rooms.findById(10L)).thenReturn(Optional.of(room()));
        when(users.findById(2L)).thenReturn(Optional.of(user(2L)));
        stubSave();

        service.addModerator(1L, 10L, 2L);
        var result = service.addModerator(1L, 10L, 2L);

        assertThat(result.moderatorIds()).containsExactly(1L, 2L);
    }

    @Test
    void ownerCanRevokeModerator() {
        var room = room();
        room.getModerators().add(user(2L));
        when(rooms.findById(10L)).thenReturn(Optional.of(room));
        stubSave();

        service.removeModerator(1L, 10L, 2L);

        assertThat(room.getModerators()).extracting(User::getId).containsExactly(1L);
        verify(events).publishEvent(new ChatEvent("ROOM_UPDATED", 10L, null));
    }

    @Test
    void ownerCannotBeRemovedFromModerators() {
        when(rooms.findById(10L)).thenReturn(Optional.of(room()));
        assertStatus(409, () -> service.removeModerator(1L, 10L, 1L));
        verifyNoInteractions(events);
    }

    @Test
    void cannotAssignNonexistentUser() {
        when(rooms.findById(10L)).thenReturn(Optional.of(room()));
        assertStatus(404, () -> service.addModerator(1L, 10L, 999L));
        verifyNoInteractions(events);
    }

    @Test
    void deletingRoomPublishesDeletionEvent() {
        var room = room();
        when(rooms.findById(10L)).thenReturn(Optional.of(room));
        service.delete(1L, 10L);
        verify(rooms).delete(room);
        verify(events).publishEvent(new ChatEvent("ROOM_DELETED", 10L, null));
    }

    @Test
    void missingRoomReturnsNotFound() {
        assertStatus(404, () -> service.get(999L));
    }

    @Test
    void rejectsBlankRoomNameWithoutSaving() {
        assertStatus(400, () -> service.create(1L, new RoomRequest(" ", null)));
        verify(rooms, never()).saveAndFlush(any());
        verifyNoInteractions(events);
    }

    private void stubSave() {
        when(rooms.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ChatRoom room() {
        var owner = user(1L);
        var room = new ChatRoom();
        room.setId(10L);
        room.setOwner(owner);
        room.setName("Travel");
        room.getModerators().add(owner);
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
