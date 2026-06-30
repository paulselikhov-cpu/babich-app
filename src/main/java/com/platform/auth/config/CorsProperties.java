package com.platform.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Читает cors.allowed-origins из application.yaml
 * и делает их доступными как обычный Java объект.
 *
 * Аналогия с Angular: это как environment.ts —
 * конфиг вынесен отдельно от кода.
 */
@Data
@Component
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private List<String> allowedOrigins;
}