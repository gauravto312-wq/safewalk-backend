package com.safewalk.security;

import com.safewalk.entity.AccountStatus;
import com.safewalk.entity.Role;
import com.safewalk.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebSocketAuthInterceptorTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @BeforeEach
    void setUp() {
        webSocketAuthInterceptor = new WebSocketAuthInterceptor(jwtTokenProvider, customUserDetailsService);
    }

    @Test
    void preSend_ValidJwtConnectHeader_SetsAuthenticatedUserPrincipal() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer valid.jwt.token");
        accessor.setLeaveMutable(true);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.validateToken("valid.jwt.token")).thenReturn(true);
        when(jwtTokenProvider.getMobileNumberFromToken("valid.jwt.token")).thenReturn("+919876543210");

        User user = User.builder()
                .id(1L)
                .name("Aman")
                .mobileNumber("+919876543210")
                .password("hash")
                .role(com.safewalk.entity.Role.USER)
                .accountStatus(com.safewalk.entity.AccountStatus.ACTIVE)
                .build();

        UserPrincipal userPrincipal = UserPrincipal.create(user);

        when(customUserDetailsService.loadUserByUsername("+919876543210")).thenReturn(userPrincipal);

        Message<?> result = webSocketAuthInterceptor.preSend(message, messageChannel);

        StompHeaderAccessor resultAccessor = StompHeaderAccessor.wrap(result);
        Principal principal = resultAccessor.getUser();
        assertNotNull(principal);
        assertEquals("+919876543210", principal.getName());
    }

    @Test
    void preSend_InvalidToken_DoesNotSetPrincipal() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setNativeHeader("Authorization", "Bearer invalid.jwt.token");
        accessor.setLeaveMutable(true);

        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        when(jwtTokenProvider.validateToken("invalid.jwt.token")).thenReturn(false);

        org.junit.jupiter.api.Assertions.assertThrows(
                org.springframework.security.access.AccessDeniedException.class,
                () -> webSocketAuthInterceptor.preSend(message, messageChannel));
    }
}
