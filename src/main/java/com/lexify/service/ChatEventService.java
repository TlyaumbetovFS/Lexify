package com.lexify.service;

import com.lexify.dto.ChatEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
public class ChatEventService {

    private final SimpMessagingTemplate messaging;

    @TransactionalEventListener
    public void publish(ChatEvent event) {
        var destination = event.message() == null ? "/topic/rooms" : "/topic/rooms/" + event.roomId();
        messaging.convertAndSend(destination, event);
    }
}
