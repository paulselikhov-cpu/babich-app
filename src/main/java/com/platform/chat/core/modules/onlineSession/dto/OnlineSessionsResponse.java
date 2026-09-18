package com.platform.chat.core.modules.onlineSession.dto;

/**
 * Событие presence, рассылаемое через /topic/location.{id}.presence.
 * Десериализуется на фронте как IPresenceEvent.
 */
public record OnlineSessionsResponse(Long characterId, boolean online, Long roomId) {};
