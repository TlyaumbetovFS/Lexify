package com.lexify.config;

import com.lexify.repository.RoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtDecoder decoder;
    private final RoomRepository rooms;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        var command = accessor.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            var header = accessor.getFirstNativeHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                throw new AuthenticationCredentialsNotFoundException("JWT required");
            }
            var jwt = decoder.decode(header.substring(7));
            accessor.setUser(new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject()));
            accessor.removeNativeHeader("Authorization");
            return message;
        }
        if (command == StompCommand.DISCONNECT || command == StompCommand.UNSUBSCRIBE) {
            return message;
        }
        if (!(accessor.getUser() instanceof JwtAuthenticationToken authentication)) {
            throw new AuthenticationCredentialsNotFoundException("JWT required");
        }
        decoder.decode(authentication.getToken().getTokenValue());
        var destination = accessor.getDestination();
        if (command == StompCommand.SEND && destination != null
                && destination.matches("/app/rooms/[1-9][0-9]*/messages")) {
            return message;
        }
        if (command == StompCommand.SUBSCRIBE && isAllowedSubscription(destination)) {
            return message;
        }
        throw new AccessDeniedException("Destination not allowed");
    }

    private boolean isAllowedSubscription(String destination) {
        if ("/user/queue/errors".equals(destination) || "/topic/rooms".equals(destination)) {
            return true;
        }
        if (destination == null || !destination.matches("/topic/rooms/[1-9][0-9]*")) {
            return false;
        }
        try {
            var roomId = Long.parseLong(destination.substring("/topic/rooms/".length()));
            return rooms.existsById(roomId);
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
