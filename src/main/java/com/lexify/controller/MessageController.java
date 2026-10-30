package com.lexify.controller;

import com.lexify.dto.ChatError;
import com.lexify.dto.MessageRequest;
import com.lexify.dto.MessageResponse;
import com.lexify.service.MessageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/rooms/{roomId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService service;

    @GetMapping
    public List<MessageResponse> list(@PathVariable Long roomId,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return service.list(roomId, page, size);
    }

    @GetMapping("/{id}")
    public MessageResponse get(@PathVariable Long roomId, @PathVariable Long id) {
        return service.get(roomId, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MessageResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId,
                                  @Valid @RequestBody MessageRequest request) {
        return service.create(Long.valueOf(jwt.getSubject()), roomId, request);
    }

    @PutMapping("/{id}")
    public MessageResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long id,
                                  @Valid @RequestBody MessageRequest request) {
        return service.update(Long.valueOf(jwt.getSubject()), roomId, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long roomId, @PathVariable Long id) {
        service.delete(Long.valueOf(jwt.getSubject()), roomId, id);
    }

    @MessageMapping("/rooms/{roomId}/messages")
    public void send(@DestinationVariable Long roomId, @Valid @Payload MessageRequest request, Principal principal) {
        service.create(Long.valueOf(principal.getName()), roomId, request);
    }

    @MessageExceptionHandler(ResponseStatusException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatError handleChatError(ResponseStatusException exception) {
        return new ChatError(exception.getStatusCode().value(), exception.getReason());
    }

    @MessageExceptionHandler({MethodArgumentNotValidException.class, MessageConversionException.class})
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public ChatError handleInvalidMessage(Exception exception) {
        return new ChatError(400, "Message text must contain between 1 and 2000 characters");
    }
}
