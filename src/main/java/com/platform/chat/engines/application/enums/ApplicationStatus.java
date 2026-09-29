package com.platform.chat.engines.application.enums;

/**
 * Статус гражданской заявки: {@code PENDING} — на рассмотрении,
 * {@code APPROVED}/{@code REJECTED} — решение принято.
 *
 * <p>Единственная точка перевода заявки в решённый статус —
 * {@code ApplicationService} (ядро процесса). Handler-стратегия лишь
 * возвращает своё решение, применяет его ядро.</p>
 */
public enum ApplicationStatus {
    PENDING,
    APPROVED,
    REJECTED
}
