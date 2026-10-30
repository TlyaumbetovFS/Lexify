package com.lexify.config;

import com.lexify.repository.RoomRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtDecoder decoder;
    @Mock
    private RoomRepository rooms;
    private WebSocketAuthInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new WebSocketAuthInterceptor(decoder, rooms);
    }

    @Test
    void connectAuthenticatesUserAndRemovesTokenHeader() {
        var accessor = accessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer token");
        when(decoder.decode("token")).thenReturn(jwt());

        interceptor.preSend(message(accessor), null);

        assertThat(accessor.getUser()).isInstanceOf(JwtAuthenticationToken.class);
        assertThat(accessor.getUser().getName()).isEqualTo("1");
        assertThat(accessor.getFirstNativeHeader("Authorization")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Basic token", "token"})
    void connectRejectsMissingBearerToken(String header) {
        var accessor = accessor(StompCommand.CONNECT);
        if (!header.isEmpty()) {
            accessor.setNativeHeader("Authorization", header);
        }
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
        verifyNoInteractions(decoder);
    }

    @Test
    void connectRejectsInvalidOrExpiredJwt() {
        var accessor = accessor(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer expired");
        when(decoder.decode("expired")).thenThrow(new BadJwtException("Expired"));
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null)).isInstanceOf(BadJwtException.class);
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void unauthenticatedUserCannotSubscribe() {
        var accessor = accessor(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/rooms");
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(AuthenticationCredentialsNotFoundException.class);
    }

    @Test
    void permitsApplicationSendForAuthenticatedUser() {
        var accessor = authenticated(StompCommand.SEND);
        accessor.setDestination("/app/rooms/10/messages");
        interceptor.preSend(message(accessor), null);
        verify(decoder).decode("token");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/rooms/10", "/queue/errors", "/user/queue/errors", "/app/anything"})
    void clientCannotPublishDirectlyToBrokerOrUnknownHandler(String destination) {
        var accessor = authenticated(StompCommand.SEND);
        accessor.setDestination(destination);
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void permitsSubscriptionToExistingRoom() {
        var accessor = authenticated(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/rooms/10");
        when(rooms.existsById(10L)).thenReturn(true);
        interceptor.preSend(message(accessor), null);
        verify(rooms).existsById(10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/rooms", "/user/queue/errors"})
    void permitsRoomListAndOwnErrorQueue(String destination) {
        var accessor = authenticated(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        interceptor.preSend(message(accessor), null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/topic/**", "/topic/rooms/*", "/queue/errors", "/user/2/queue/errors", "/topic/rooms/999999999999999999999999"})
    void rejectsWildcardSubscriptionsAndOtherUsersQueues(String destination) {
        var accessor = authenticated(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsSubscriptionToMissingRoom() {
        var accessor = authenticated(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/rooms/999");
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rechecksTokenBeforeSending() {
        var accessor = accessor(StompCommand.SEND);
        accessor.setUser(new JwtAuthenticationToken(jwt()));
        accessor.setDestination("/app/rooms/10/messages");
        when(decoder.decode("token")).thenThrow(new BadJwtException("Expired"));
        assertThatThrownBy(() -> interceptor.preSend(message(accessor), null)).isInstanceOf(BadJwtException.class);
    }

    private StompHeaderAccessor authenticated(StompCommand command) {
        var accessor = accessor(command);
        accessor.setUser(new JwtAuthenticationToken(jwt()));
        when(decoder.decode("token")).thenReturn(jwt());
        return accessor;
    }

    private StompHeaderAccessor accessor(StompCommand command) {
        var accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("test-session");
        accessor.setLeaveMutable(true);
        return accessor;
    }

    private Message<byte[]> message(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Jwt jwt() {
        return Jwt.withTokenValue("token").header("alg", "HS256").subject("1")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(3600)).build();
    }
}
