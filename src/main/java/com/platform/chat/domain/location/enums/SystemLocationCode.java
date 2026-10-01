package com.platform.chat.domain.location.enums;

import java.util.Set;

/**
 * Коды системных локаций района — хранятся в {@code Location.code}.
 *
 * <p>Уникальность кода составная: {@code UNIQUE (district_id, code)}.
 * У каждого района своя «Площадь Ленина», поэтому код повторяется между
 * районами — как «сервер» в онлайн-игре, к которому подключается персонаж.
 * Поэтому код резолвится ТОЛЬКО в пределах района текущего персонажа:
 * {@code GET /api/locations/by-code/{code}} берёт район из principal, а не из URL.
 *
 * <p>Эти значения запрещены для пользовательских локаций (проверка в
 * {@code LocationService.createLocation}) — иначе игрок занял бы код мэрии.</p>
 */
public final class SystemLocationCode {

    /** Мэрия — управление районом. */
    public static final String CITY_HALL = "CITY_HALL";
    /** Площадь Ленина — народные голосования + комната «Тёплые трубы» для бомжей. */
    public static final String LENIN_SQUARE = "LENIN_SQUARE";
    /** Полицейский участок — база должности POLICE_OFFICER. */
    public static final String POLICE_STATION = "POLICE_STATION";
    /** Тюрьма. */
    public static final String PRISON = "PRISON";
    /** Банк — должности BANK_DIRECTOR / BANK_EMPLOYEE. */
    public static final String BANK = "BANK";
    /** Склад — работает только для бизнесменов. */
    public static final String WAREHOUSE = "WAREHOUSE";
    /** Магазин. */
    public static final String GENERAL_MARKET = "GENERAL_MARKET";
    /** Рынок недвижимости — должность REAL_ESTATE_DIRECTOR. */
    public static final String REAL_ESTATE_MARKET = "REAL_ESTATE_MARKET";

    private static final Set<String> ALL = Set.of(
            CITY_HALL, LENIN_SQUARE, POLICE_STATION, PRISON,
            BANK, WAREHOUSE, GENERAL_MARKET, REAL_ESTATE_MARKET
    );

    private SystemLocationCode() {
    }

    /** Зарезервирован ли код под системную локацию. */
    public static boolean isReserved(String code) {
        return code != null && ALL.contains(code);
    }

    /** Все системные коды. */
    public static Set<String> all() {
        return ALL;
    }

    /**
     * Человекочитаемое имя системной локации по коду (для сидера и логов).
     * Для неизвестного кода возвращается сам код.
     */
    public static String displayName(String code) {
        return switch (code) {
            case CITY_HALL -> "Мэрия";
            case LENIN_SQUARE -> "Площадь Ленина";
            case POLICE_STATION -> "Полицейский участок";
            case PRISON -> "Тюрьма";
            case BANK -> "Банк";
            case WAREHOUSE -> "Склад";
            case GENERAL_MARKET -> "Магазин";
            case REAL_ESTATE_MARKET -> "Рынок недвижимости";
            default -> code;
        };
    }
}