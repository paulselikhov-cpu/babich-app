package com.platform.chat.engines.webSocket.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // префикс для топиков на которые подписываются клиенты
        // Angular будет слушать /topic/messages, /topic/users и тд
        registry.enableSimpleBroker("/topic", "/queue")
        // сервер и клиент проверяют друг друга каждые 10 сек на уровне
        // самого протокола STOMP — если клиент не ответил дважды подряд,
        // Spring сам инициирует SessionDisconnectEvent, не дожидаясь
        // 45-секундного backstop-таймера в OnlineSessionService
        .setHeartbeatValue(new long[]{10_000, 10_000})
        .setTaskScheduler(heartbeatScheduler());
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
                .setAllowedOriginPatterns("*"); // CORS для WS
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
    }

    private TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}