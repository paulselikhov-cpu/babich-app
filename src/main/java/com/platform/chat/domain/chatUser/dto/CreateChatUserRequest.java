package com.platform.chat.base.chatUser.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChatUserRequest {

    @NotBlank(message = "Имя персонажа обязательно")
    @Size(min = 2, max = 50, message = "Имя персонажа должно быть от 2 до 50 символов")
    private String characterName;

    @NotNull(message = "Район обязателен")
    private Long districtId;

    private String gender;
    private Double weight;
    private Double height;
    private String badHabits;
    private String avatarUrl;
}