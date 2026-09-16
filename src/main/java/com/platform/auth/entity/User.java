package com.platform.auth.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.modulith.NamedInterface;

import java.time.LocalDateTime;

@NamedInterface("entityUser")
@Data               // Lombok: генерирует геттеры, сеттеры, toString автоматически
@Entity             // говорит Hibernate — это таблица в БД
@Table(name = "users") // название таблицы (без аннотации = название класса)
@Inheritance(strategy = InheritanceType.JOINED)  // ← обязательно на родителе
public class User {

    @Id                                                    // первичный ключ
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // автоинкремент (как SERIAL в PostgreSQL)
    private Long id;

    @Column(unique = true, nullable = false)   // уникальный, обязательный
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist   // вызывается автоматически перед сохранением в БД
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
