package com.platform.chat.core.modules.chatRoom.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.location.entity.Location;
import com.platform.chat.base.room.entity.Room;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * Присутствие персонажа — ГДЕ он сейчас онлайн.
 *
 * Одна запись НА ПЕРСОНАЖА (character_id уникален), а не на сессию/локацию.
 * Если персонаж открывает вторую вкладку — она просто перезаписывает эту
 * же строку новым sessionId. Первая вкладка становится "немой" и сама
 * протухнет по heartbeat-таймауту (см. {@code OnlineSessionService.onTick}).
 *
 * room_id NOT NULL — по бизнес-правилу пользователь online всегда находится
 * в какой-то комнате (минимум — в дефолтной "прихожей" локации).
 */
@Data
@Entity
@Table(name = "character_presence")
public class OnlineSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "character_id", nullable = false, unique = true) //зачем дублирование
    private Long characterId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", insertable = false, updatable = false)
    private ChatUser character; //зачем дублирование

    /**
     * ID текущей STOMP-сессии. Нужен, чтобы при дисконнекте понять,
     * какую именно вкладку закрыли — актуальную или уже "перезахваченную"
     * второй вкладкой того же персонажа (см. handleDisconnect).
     */
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false) // всегда есть — "без комнаты" не бывает
    private Room room;

    @Column(name = "entered_at")
    private LocalDateTime enteredAt;

    /**
     * Обновляется каждым heartbeat от клиента (раз в 10 сек).
     * Если не обновлялась дольше HEARTBEAT_TIMEOUT_SECONDS — считаем
     * сессию мёртвой, даже если close-фрейм WebSocket не пришёл
     * (обрыв сети, сон ноутбука, kill процесса).
     */
    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;

    @PrePersist
    public void prePersist() {
        this.enteredAt = LocalDateTime.now();
        this.lastHeartbeatAt = LocalDateTime.now();
    }
}