package com.platform.chat.entity;

import com.platform.auth.entity.User;
import com.platform.chat.enums.ChatRole;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "chat_users")
@PrimaryKeyJoinColumn(name = "user_id")
public class ChatUser extends User {

    @Column(name = "character_name", nullable = false)
    private String characterName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatRole role = ChatRole.USER;

    @Column(nullable = false)
    private Integer rating = 0;

    private String gender;

    private Double weight;

    private Double height;

    @Column(name = "bad_habits")
    private String badHabits;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
}