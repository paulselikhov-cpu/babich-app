package com.platform.chat.dto.events;

/**
 * Событие presence, рассылаемое через /topic/location.{id}.presence.
 * Десериализуется на фронте как IPresenceEvent.
 */
public record PresenceEvent(Long characterId, boolean online, Long roomId) {};
