package com.lexify.service;

import com.lexify.dto.ChatEvent;
import com.lexify.dto.RoomRequest;
import com.lexify.dto.RoomResponse;
import com.lexify.mapper.RoomMapper;
import com.lexify.model.ChatRoom;
import com.lexify.repository.RoomRepository;
import com.lexify.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository rooms;
    private final UserRepository users;
    private final RoomMapper mapper;
    private final ApplicationEventPublisher events;

    public List<RoomResponse> list() {
        return rooms.findAllByOrderByIdDesc().stream().map(mapper::toResponse).toList();
    }

    public RoomResponse get(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public RoomResponse create(Long userId, RoomRequest request) {
        var owner = users.getReferenceById(userId);
        var room = new ChatRoom();
        room.setOwner(owner);
        room.getModerators().add(owner);
        room.setName(normalizeName(request.name()));
        room.setDescription(request.description());
        var saved = rooms.saveAndFlush(room);
        events.publishEvent(new ChatEvent("ROOM_CREATED", saved.getId(), null));
        return mapper.toResponse(saved);
    }

    @Transactional
    public RoomResponse update(Long userId, Long id, RoomRequest request) {
        var room = findOwned(userId, id);
        room.setName(normalizeName(request.name()));
        room.setDescription(request.description());
        return saveUpdated(room);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        rooms.delete(findOwned(userId, id));
        events.publishEvent(new ChatEvent("ROOM_DELETED", id, null));
    }

    @Transactional
    public RoomResponse addModerator(Long userId, Long id, Long moderatorId) {
        var room = findOwned(userId, id);
        var moderator = users.findById(moderatorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        room.getModerators().add(moderator);
        return saveUpdated(room);
    }

    @Transactional
    public void removeModerator(Long userId, Long id, Long moderatorId) {
        var room = findOwned(userId, id);
        if (room.getOwner().getId().equals(moderatorId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Room owner must remain a moderator");
        }
        room.getModerators().removeIf(user -> user.getId().equals(moderatorId));
        saveUpdated(room);
    }

    private RoomResponse saveUpdated(ChatRoom room) {
        var saved = rooms.saveAndFlush(room);
        events.publishEvent(new ChatEvent("ROOM_UPDATED", saved.getId(), null));
        return mapper.toResponse(saved);
    }

    private ChatRoom find(Long id) {
        return rooms.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
    }

    private ChatRoom findOwned(Long userId, Long id) {
        var room = find(id);
        if (!room.getOwner().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the room owner can manage this room");
        }
        return room;
    }

    private String normalizeName(String name) {
        var normalized = name.strip();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Room name must not be blank");
        }
        return normalized;
    }
}
