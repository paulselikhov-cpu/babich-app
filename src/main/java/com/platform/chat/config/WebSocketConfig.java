package com.platform.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // префикс для топиков на которые подписываются клиенты
        // Angular будет слушать /topic/messages, /topic/users и тд
        registry.enableSimpleBroker("/topic", "/queue");

        // префикс для сообщений от клиента к серверу
        // Angular будет отправлять на /app/chat.send и тд
        registry.setApplicationDestinationPrefixes("/app");

        // префикс для личных сообщений конкретному юзеру
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")           // endpoint к которому подключается клиент
                .setAllowedOriginPatterns("*") // CORS для WS
                .withSockJS();                 // fallback для браузеров без нативного WS
    }
}