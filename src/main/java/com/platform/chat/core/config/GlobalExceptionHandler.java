package com.platform.chat.core.config;

import com.platform.chat.core.modules.application.exception.ApplicationAlreadyResolvedException;
import com.platform.chat.core.modules.application.exception.ApplicationNotFoundException;
import com.platform.chat.core.modules.application.exception.ApplicationSubmissionRejectedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Глобальный маппинг необработанных бизнес-ошибок в HTTP-ответы.
 * Фронтенд читает поле message из тела ошибки (err?.error?.message) —
 * поэтому бизнес-исключения с текстом отдаются как 400 с message, а не 500.
 * Исключения без текста (NPE и т.п.) остаются 500 — не маскируем баги.
 *
 * Локальные @ExceptionHandler контроллеров (WorkController, ElectionController)
 * имеют приоритет над этим advice и продолжают работать как раньше.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Ошибка бизнес-логики с человеческим текстом (сервисы бросают RuntimeException с текстом). */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        if (ex.getMessage() == null || ex.getMessage().isBlank()) {
            // неожиданный баг (NPE и т.п.) — остаётся 500, логируем полностью
            log.error("Необработанное исключение без текста", ex);
            return build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");
        }
        log.warn("Бизнес-ошибка: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Битый enum/число в @RequestParam — ошибка клиента, а не сервера. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Неразборчивый параметр запроса: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "Некорректный параметр запроса");
    }

    /** Заявка не найдена (approve/reject по несуществующему id) — ошибка клиента. */
    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationNotFound(ApplicationNotFoundException ex) {
        log.warn("Заявка не найдена: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Заявка уже решена — повторная попытка модерации той же заявки. */
    @ExceptionHandler(ApplicationAlreadyResolvedException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationAlreadyResolved(
            ApplicationAlreadyResolvedException ex) {
        log.warn("Заявка уже обработана: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Подача заявки отклонена бизнес-правилом (дубль документа, незакрытая заявка). */
    @ExceptionHandler(ApplicationSubmissionRejectedException.class)
    public ResponseEntity<Map<String, Object>> handleSubmissionRejected(
            ApplicationSubmissionRejectedException ex) {
        log.warn("Подача заявки отклонена: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Нет прав на действие (модерация заявки не сотрудником мэрии) — 403.
     * Фронт по коду 403 показывает «нет доступа» (см. employee-applications-menu).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Доступ запрещён: {}", ex.getMessage());
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    /** Нарушение целостности БД (уникальные ограничения location_users и т.п.) — конфликт. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException ex) {
        log.warn("Нарушение целостности данных: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Конфликт данных: запись уже существует");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", java.time.LocalDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
