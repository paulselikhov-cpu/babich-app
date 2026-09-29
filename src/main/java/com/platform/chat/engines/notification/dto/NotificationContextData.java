package com.platform.chat.core.modules.notification.dto;

import com.platform.chat.core.modules.notification.service.NotificationService;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * //TODO ПОДУМАТЬ НУЖЕН ЛИ ДАННЫЙ ФУНКЦИОНАЛ
 * Компактное текстовое представление контекстных данных уведомления
 * (поле {@code Notification.data}).
 *
 * <p>Формат хранения — {@code key=value;key2=value2} (например,
 * {@code applicationType=REGISTER_PASSPORT;applicationStatus=APPROVED}).
 * Это простая сериализация без JSONB и без привязки к Jackson — по духу того,
 * как {@code Application.payload} уже хранится на этапе MVP.</p>
 *
 * <p>Значения тут — имена enum (безопасный алфавит), но на всякий случай
 * {@code \}, {@code =} и {@code ;} экранируются при записи и снимаются при
 * чтении, чтобы держать формат корректным для произвольных строк.</p>
 *
 * <p>Используется и в {@link NotificationService}
 * (запись), и в {@link NotificationResponse} (чтение в WS/REST), чтобы live-пуш
 * и REST-история были одинаковой формы.</p>
 */
public final class NotificationContextData {

    private static final char ESCAPE = '\\';
    private static final char KV_SEP = '=';
    private static final char ENTRY_SEP = ';';

    private NotificationContextData() {
    }

    /** Сериализовать контекстные данные в строку (null при пустой/отсутствующей мапе). */
    public static String toText(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : data.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(ENTRY_SEP);
            }
            sb.append(escape(e.getKey())).append(KV_SEP)
              .append(escape(e.getValue() == null ? null : String.valueOf(e.getValue())));
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /** Распарсить строку контекстных данных в Map (null при null/пустой строке). */
    public static Map<String, Object> parse(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (String entry : splitEntries(text)) {
            int sep = entry.indexOf(KV_SEP);
            if (sep <= 0) {
                continue;
            }
            String key = unescape(entry.substring(0, sep));
            String value = unescape(entry.substring(sep + 1));
            result.put(key, value);
        }
        return result.isEmpty() ? null : result;
    }

    private static String[] splitEntries(String text) {
        // разбиваем по «;», но не внутри экранированного «\;»
        java.util.List<String> parts = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                cur.append(c);
                escaped = false;
            } else if (c == ESCAPE) {
                cur.append(c);
                escaped = true;
            } else if (c == ENTRY_SEP) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(new String[0]);
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ESCAPE || c == KV_SEP || c == ENTRY_SEP) {
                sb.append(ESCAPE);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static String unescape(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!escaped && c == ESCAPE) {
                escaped = true;
            } else {
                sb.append(c);
                escaped = false;
            }
        }
        // если строка заканчивалась на экранирующий backslash — оставляем его как есть
        if (escaped) {
            sb.append(ESCAPE);
        }
        return sb.toString();
    }
}