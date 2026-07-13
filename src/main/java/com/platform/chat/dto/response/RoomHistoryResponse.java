package com.platform.chat.dto.response;

import java.util.List;

/**
 * Ответ на загрузку истории комнаты.
 * firstUnreadMessageId — id первого сообщения, которое пользователь ещё
 * не видел (т.е. написанного ПОСЛЕ его предыдущего визита). Null, если
 * непрочитанных нет — тогда разделитель на фронте не рисуется вообще.
 */
public record RoomHistoryResponse(
        List<ChatMessageResponse> messages,
        Long firstUnreadMessageId,
        long unreadCount
) {}