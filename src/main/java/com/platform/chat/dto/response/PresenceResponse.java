package com.platform.chat.dto.response;

/**
 * Событие presence, рассылаемое через /topic/location.{id}.presence.
 * Десериализуется на фронте как IPresenceEvent.
 */
public record PresenceResponse(Long characterId, boolean online, Long roomId) {};
