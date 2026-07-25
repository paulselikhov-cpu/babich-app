package com.platform.civic.service;

import com.platform.civic.entity.Application;
import com.platform.civic.enums.ApplicationType;
import com.platform.civic.handler.ApplicationHandler;
import com.platform.civic.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Диспетчер обработки заявок. Находит подходящий ApplicationHandler
 * по типу заявки (стратегия) и делегирует ему бизнес-логику.
 * Сам сервис не знает деталей — просто создаёт заявку, вызывает
 * обработчик и сохраняет результат.
 */
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final List<ApplicationHandler> handlers;

    private Map<ApplicationType, ApplicationHandler> handlerMap;

    private Map<ApplicationType, ApplicationHandler> getHandlerMap() {
        if (handlerMap == null) {
            handlerMap = handlers.stream()
                    .collect(Collectors.toMap(ApplicationHandler::getType, h -> h));
        }
        return handlerMap;
    }

    @Transactional
    public Application submit(Long userId, ApplicationType type, String payload) {
        Application application = new Application();
        application.setUserId(userId);
        application.setType(type);
        application.setPayload(payload);
        applicationRepository.save(application);

        ApplicationHandler handler = getHandlerMap().get(type);
        if (handler == null) {
            throw new RuntimeException("Нет обработчика для типа заявки: " + type);
        }

        handler.handle(application);
        application.setProcessedAt(LocalDateTime.now());
        return applicationRepository.save(application);
    }

    public List<Application> getMyApplications(Long userId) {
        return applicationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
