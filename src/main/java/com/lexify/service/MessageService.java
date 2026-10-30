package com.lexify.service;

import com.lexify.dto.ChatEvent;
import com.lexify.dto.MessageRequest;
import com.lexify.dto.MessageResponse;
import com.lexify.mapper.MessageMapper;
import com.lexify.model.ChatMessage;
import com.lexify.repository.MessageRepository;
import com.lexify.repository.RoomRepository;
import com.lexify.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private final MessageRepository messages;
    private final RoomRepository rooms;
    private final UserRepository users;
    private final MessageMapper mapper;
    private final ApplicationEventPublisher events;

    public List<MessageResponse> list(Long roomId, int page, int size) {
        if (!rooms.existsById(roomId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        return messages.findByRoomIdOrderByIdDesc(roomId, PageRequest.of(page, size))
                .stream().map(mapper::toResponse).toList();
    }

    public MessageResponse get(Long roomId, Long id) {
        return mapper.toResponse(find(roomId, id));
    }

    @Transactional
    public MessageResponse create(Long userId, Long roomId, MessageRequest request) {
        var room = rooms.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Room not found"));
        var message = new ChatMessage();
        message.setRoom(room);
        message.setAuthor(users.getReferenceById(userId));
        message.setText(normalizeText(request.text()));
        message.setCreatedAt(Instant.now());
        return save(message, "MESSAGE_CREATED");
    }

    @Transactional
    public MessageResponse update(Long userId, Long roomId, Long id, MessageRequest request) {
        var message = find(roomId, id);
        if (!message.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author can edit this message");
        }
        message.setText(normalizeText(request.text()));
        return save(message, "MESSAGE_UPDATED");
    }

    @Transactional
    public void delete(Long userId, Long roomId, Long id) {
        var message = find(roomId, id);
        if (!message.getAuthor().getId().equals(userId) && !rooms.existsByIdAndModeratorsId(roomId, userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the author or a moderator can delete this message");
        }
        var response = mapper.toResponse(message);
        messages.delete(message);
        events.publishEvent(new ChatEvent("MESSAGE_DELETED", roomId, response));
    }

    private MessageResponse save(ChatMessage message, String type) {
        var response = mapper.toResponse(messages.saveAndFlush(message));
        events.publishEvent(new ChatEvent(type, response.roomId(), response));
        return response;
    }

    private ChatMessage find(Long roomId, Long id) {
        return messages.findByIdAndRoomId(id, roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found"));
    }

    private String normalizeText(String text) {
        var normalized = text.strip();
        if (normalized.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Message must not be blank");
        }
        return normalized;
    }
}
