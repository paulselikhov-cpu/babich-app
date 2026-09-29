package com.platform.chat.engines.webSocket.config;

import com.platform.auth.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    /**
     * Текст, который уезжает клиенту в заголовке {@code message} STOMP-ERROR-фрейма.
     * Фронт (StompConnectionService.onStompError) ищет в нём '401'/'Unauthorized' —
     * иначе пользователь не разлогинится, а уйдёт в бесконечный реконнект со старым токеном.
     */
    private static final String UNAUTHORIZED = "401 Unauthorized";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // перехватываем только CONNECT — он устанавливает Principal на всю сессию
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String token = extractToken(accessor);
        if (token == null) {
            throw reject("отсутствует заголовок Authorization в CONNECT-фрейме");
        }

        // extractUsername сам парсит токен, поэтому он и есть проверка валидности:
        // на истёкшем/битом токене бросает JwtException. Ловить его здесь обязательно —
        // иначе исключение улетает из clientInboundChannel, Spring закрывает сессию
        // кодом 1002 и шлёт клиенту ERROR с generic-текстом, по которому тот не понимает,
        // что дело в авторизации, и уходит в бесконечный реконнект.
        String username;
        try {
            username = jwtService.extractUsername(token);
        }
        catch (ExpiredJwtException e) {
            throw reject("JWT истёк " + e.getClaims().getExpiration().toInstant());
        }
        catch (JwtException | IllegalArgumentException e) {
            throw reject("невалидный JWT: " + e.getMessage());
        }

        try {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth); // ← вот откуда берётся Principal
        }
        catch (UsernameNotFoundException e) {
            throw reject("пользователь '" + username + "' не найден");
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    /**
     * Отказ в CONNECT. Кидаем именно {@link MessagingException} (а не произвольное
     * исключение): {@code AbstractMessageChannel.send} перебрасывает MessagingException
     * как есть, а {@code StompSubProtocolHandler.sendErrorMessage} кладёт его
     * getMessage() в заголовок {@code message} STOMP-ERROR-фрейма. Так клиент вместо
     * generic-текста получает осмысленный "401 Unauthorized" и корректно разлогинивается.
     */
    private MessagingException reject(String reason) {
        log.warn("WS CONNECT отклонён: {}", reason);
        return new MessagingException(UNAUTHORIZED + ": " + reason);
    }
}