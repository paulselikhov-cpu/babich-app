package com.platform.chat.dto.events;

public record LocationPresenceCountEvent(Long locationId, boolean online) {}
