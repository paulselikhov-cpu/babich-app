package com.platform.chat.core.modules.chatRoom;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.base.district.entity.District;
import com.platform.chat.base.district.repository.DistrictRepository;
import com.platform.chat.base.location.entity.Location;
import com.platform.chat.base.location.entity.LocationUser;
import com.platform.chat.base.location.enums.LocationCategoryType;
import com.platform.chat.base.location.enums.LocationUserRole;
import com.platform.chat.base.location.repository.LocationRepository;
import com.platform.chat.base.location.repository.LocationUserRepository;
import com.platform.chat.base.location.service.LocationService;
import com.platform.chat.base.room.entity.Room;
import com.platform.chat.base.room.repository.RoomReadStatusRepository;
import com.platform.chat.base.room.repository.RoomRepository;
import com.platform.chat.core.modules.chatRoom.repository.MessageRepository;
import com.platform.chat.core.modules.chatRoom.repository.OnlineSessionRepository;
import com.platform.chat.core.modules.chatRoom.service.MessageService;
import com.platform.chat.core.modules.chatRoom.service.OnlineSessionService;
import com.platform.chat.core.modules.chatRoom.service.UnreadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сквозные тесты слайса «чат комнаты» ({@code chat.core.modules.chatRoom}).
 *
 * <p>Покрывают то, что раньше было вообще без тестов (два раздельных слайса
 * message/onlineSession): непрочитанное, отметку чтения, присутствие и связку
 * «удаление локации → чистка содержимого комнат через порт
 * {@code base.room.port.RoomContentPurger}» (раньше это делал JPA-каскад
 * упразднённой связи {@code Room.messages}).</p>
 *
 * <p>Тест не транзакционный: данные коммитятся в dev-БД (как в остальных
 * интеграционных тестах проекта), поэтому все имена/id уникальны на каждый
 * прогон.</p>
 */
@SpringBootTest
class ChatRoomIntegrationTest {

    @Autowired
    private MessageService messageService;

    @Autowired
    private UnreadService unreadService;

    @Autowired
    private OnlineSessionService onlineSessionService;

    @Autowired
    private LocationService locationService;

    @Autowired
    private ChatUserRepository chatUserRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private LocationUserRepository locationUserRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomReadStatusRepository readStatusRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private OnlineSessionRepository onlineSessionRepository;

    /** Владелец локации — автор сообщений. */
    private ChatUser author;

    /** Второй персонаж — получатель (у него считаем непрочитанное). */
    private ChatUser reader;

    private Location location;

    private Room room;

    @BeforeEach
    void setUp() {
        long unique = System.nanoTime();
        String name = "ChatRoomTest_" + unique;

        District district = districtRepository.findAll().stream().findFirst().orElseGet(() -> {
            District created = new District();
            created.setName(name);
            created.setCode(String.valueOf(unique % 100_000_000L));
            return districtRepository.save(created);
        });

        author = createCharacter(name + "_author", 10_000L, district);
        reader = createCharacter(name + "_reader", 20_000L, district);

        Location created = new Location();
        created.setName(name);
        created.setDistrict(district);
        created.setIsSystem(false);
        created.setCategory(LocationCategoryType.BUSINESS);
        created.setIsPublic(false);
        created.setOwner(author);
        created.setInviteCode(String.valueOf(unique % 1_000_000_000_000L));
        location = locationRepository.save(created);

        Room createdRoom = new Room();
        createdRoom.setLocation(location);
        createdRoom.setName(name + "_room");
        createdRoom.setOrderIndex(0);
        room = roomRepository.save(createdRoom);

        LocationUser membership = new LocationUser();
        membership.setLocation(location);
        membership.setCharacter(author);
        membership.setRole(LocationUserRole.OWNER);
        membership.setIsRegistered(true);
        locationUserRepository.save(membership);
    }

    private ChatUser createCharacter(String username, Long userIdBase, District district) {
        ChatUser user = new ChatUser();
        user.setUserId(userIdBase + System.nanoTime() % 1_000_000L);
        user.setCharacterName(username);
        user.setUsername(username);
        user.setDistrictId(district.getId());
        user.setCoinBalance(1000L);
        user.setXpUser(0L);
        user.setGameStatus("HOMELESS");
        return chatUserRepository.save(user);
    }

    @Test
    @DisplayName("Непрочитанное: чужие сообщения считаются, свои — нет")
    void unreadCount_countsOthersMessagesOnly() {
        messageService.saveByUser(author, "привет", String.valueOf(room.getId()), null);

        assertThat(unreadService.getUnreadCount(reader.getId(), room.getId())).isEqualTo(1L);
        assertThat(unreadService.getUnreadCount(author.getId(), room.getId())).isZero();
    }

    @Test
    @DisplayName("Отметка чтения сбрасывает непрочитанное в 0")
    void markRead_resetsUnreadCount() {
        messageService.saveByUser(author, "привет", String.valueOf(room.getId()), null);
        assertThat(unreadService.getUnreadCount(reader.getId(), room.getId())).isEqualTo(1L);

        unreadService.markRead(reader.getId(), room.getId());

        assertThat(unreadService.getUnreadCount(reader.getId(), room.getId())).isZero();
        assertThat(readStatusRepository.findByCharacterIdAndRoomId(reader.getId(), room.getId())).isPresent();
    }

    @Test
    @DisplayName("Присутствие: вход в комнату создаёт сессию, выход — удаляет")
    void enterRoom_thenLeave_removesPresence() {
        String sessionId = "sess-" + System.nanoTime();
        onlineSessionService.enterRoom(sessionId, reader.getId(), location.getId(), room.getId());

        assertThat(onlineSessionRepository.findByCharacterId(reader.getId()))
                .hasValueSatisfying(session -> {
                    assertThat(session.getRoom().getId()).isEqualTo(room.getId());
                    assertThat(session.getSessionId()).isEqualTo(sessionId);
                });
        assertThat(onlineSessionService.getOnlineCount(location.getId())).isEqualTo(1L);

        onlineSessionService.leaveRoom(reader.getId(), location.getId(), room.getId());

        assertThat(onlineSessionRepository.findByCharacterId(reader.getId())).isEmpty();
        assertThat(onlineSessionService.getOnlineCount(location.getId())).isZero();
    }

    @Test
    @DisplayName("Присутствие: дисконнект по sessionId удаляет сессию")
    void handleDisconnect_removesPresence() {
        String sessionId = "sess-" + System.nanoTime();
        onlineSessionService.enterRoom(sessionId, reader.getId(), location.getId(), room.getId());

        onlineSessionService.handleDisconnect(sessionId);

        assertThat(onlineSessionRepository.findByCharacterId(reader.getId())).isEmpty();
    }

    @Test
    @DisplayName("Удаление локации чистит сообщения и статусы чтения (порт RoomContentPurger вместо бывшего cascade)")
    void deleteLocation_purgesRoomContent() {
        Long roomId = room.getId();
        messageService.saveByUser(author, "сообщение 1", String.valueOf(roomId), null);
        messageService.saveByUser(author, "сообщение 2", String.valueOf(roomId), null);
        unreadService.markRead(reader.getId(), roomId);
        assertThat(messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(0, 10)).getTotalElements()).isEqualTo(2L);

        locationService.deleteLocation(location.getId(), author.getId());

        assertThat(messageRepository.findByRoomIdOrderByCreatedAtDesc(
                roomId, PageRequest.of(0, 10)).getTotalElements()).isZero();
        assertThat(readStatusRepository.findByCharacterIdAndRoomId(reader.getId(), roomId)).isEmpty();
        assertThat(roomRepository.findById(roomId)).isEmpty();
        assertThat(locationRepository.findById(location.getId())).isEmpty();
    }
}
