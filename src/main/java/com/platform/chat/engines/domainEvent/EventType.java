package com.platform.chat.engines.domainEvent;

/**
 * Реестр всех событий. Подумай: что произошло — одно именованное событие. Не «что я хочу сделать
 * потом», а «что уже случилось и о чём мир должен узнать».
 */
public enum EventType {
    APPLICATION_RESOLVED,
    ELECTION_STARTED,
    ELECTION_CLOSED,
    COINS_CHANGED,
    XP_CHANGED,
    NOTIFICATION_SENT,
    CAMPAIGN_FINISHED,
    POLL_CREATED,
    POLL_CLOSED,
    CHARACTER_UPDATED
}