package com.platform.chat.domain.location.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO для создания локации.
 * Пользователь передаёт только необходимые данные —
 * id шаблона, код, название, описание и тип доступа.
 * Остальное (invite_code, owner, district, rooms) генерируется на сервере.
 */
@Data
public class CreateLocationRequest {

    /**
     * id шаблона из которого создаём локацию.
     * Пользователь выбирает шаблон в UI и передаёт его id.
     */
    @NotNull(message = "Шаблон обязателен")
    private Long templateId;

    /**
     * Код локации — придумывает игрок в форме создания (/chat/location/create).
     * Это адрес локации в URL: /chat/location/{code}.
     *
     * <p>Требования: 3–50 символов, латинские буквы, цифры и «_».
     * Сервер приводит к верхнему регистру, проверяет, что код не зарезервирован
     * под системную локацию и не занят в районе (см. LocationService).</p>
     */
    @NotBlank(message = "Код локации обязателен")
    @Size(min = 3, max = 50, message = "Код от 3 до 50 символов")
    private String code;

    /**
     * Кастомное название локации — пользователь придумывает сам.
     * Например: "Квартира Ивана", "Клуб 'Рюмочная'"
     */
    @NotBlank(message = "Название обязательно")
    @Size(min = 2, max = 100, message = "Название от 2 до 100 символов")
    private String name;

    /**
     * Описание локации — необязательное.
     */
    @Size(max = 500, message = "Описание не более 500 символов")
    private String description;

    /**
     * Публичная или приватная локация.
     * true = видна всем в общем списке
     * false = только по прямой ссылке
     */
    @NotNull(message = "Укажите тип доступа")
    private Boolean isPublic;

    /**
     * id персонажа который создаёт локацию.
     * В будущем будем брать из JWT токена автоматически —
     * пока передаём явно для простоты.
     */
    @NotNull(message = "Id персонажа обязателен")
    private Long characterId;
}