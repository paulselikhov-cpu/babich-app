package com.platform.chat.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateChatUserRequest {

    @NotBlank(message = "Имя персонажа обязательно")
    @Size(min = 2, max = 50)
    private String characterName;

    private String gender;
    private Double weight;
    private Double height;
    private String badHabits;
    private String avatarUrl;
}