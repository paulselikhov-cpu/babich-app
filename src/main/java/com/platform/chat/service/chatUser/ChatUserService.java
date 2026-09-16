package com.platform.chat.service.chatUser;

import com.platform.auth.repository.UserRepository;
import com.platform.chat.dto.request.CreateChatUserRequest;
import com.platform.chat.dto.request.CharacterUpdateRequest;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.Location;
import com.platform.chat.entity.LocationMember;
import com.platform.chat.enums.ChatRole;
import com.platform.chat.enums.LocationMemberRole;
import com.platform.chat.modules.economy.enums.Profession;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.location.LocationMemberRepository;
import com.platform.chat.repository.location.LocationRepository;
import com.platform.chat.service.level.PersonLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUserService {

    private final ChatUserRepository chatUserRepository;
    private final UserRepository userRepository;
    private final LocationRepository locationRepository;
    private final LocationMemberRepository locationMemberRepository;
    private final PersonLevelService personLevelService;

    // получить всех персонажей
    public List<ChatUser> getAllChatUsers() {
        return chatUserRepository.findAll();
    }

    // получить персонажа по id
    public ChatUser getChatUserById(Long id) {
        return chatUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + id));
    }

    public Long getChatUserLevelById(Long id) {
        ChatUser chatUser = chatUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + id));
        return personLevelService.getLevelNumber(chatUser);
    }

    /**
     * Порог XP следующего уровня персонажа (для прогресс-бара на UI).
     */
    public Long getChatUserXpThresholdById(Long id) {
        ChatUser chatUser = chatUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + id));
        return personLevelService.nextLevelXpThreshold(chatUser);
    }

    // получить персонажа пользователя в конкретном районе
    public ChatUser getChatUserByUserIdAndDistrictId(Long userId, Long districtId) {
        return chatUserRepository.findByUserIdAndDistrictId(userId, districtId)
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж для userId=" + userId + " в районе " + districtId + " не найден"));
    }

    // получить персонажа пользователя в районе (Optional)
    public Optional<ChatUser> findChatUserByUserIdAndDistrictId(Long userId, Long districtId) {
        return chatUserRepository.findByUserIdAndDistrictId(userId, districtId);
    }

    public ChatUser getChatUserByUserName(String userName) {
        ChatUser chatUser = chatUserRepository.findByUsername(userName).orElse(null);
        if (chatUser == null) {
            log.debug("Пользователь {} не найден в системе", userName);
            return null;
        }
        return chatUser;
    }

    /** Все персонажи района (для админской таблицы). */
    public List<ChatUser> listByDistrict(Long districtId) {
        return chatUserRepository.findByDistrictId(districtId);
    }

    /** Сменить роль персонажа (админская панель). Свою роль можно менять,
     *  но нельзя снять последнего администратора района. */
    @Transactional
    public ChatUser updateRole(Long characterId, ChatRole role) {
        return updateCharacterSettings(characterId, new CharacterUpdateRequest(role, null, null, null, null, null, null));
    }

    /**
     * Обновить поля персонажа из админской таблицы «Персонажи района».
     * null в запросе означает «не менять».
     * Роль меняется с защитой: нельзя снять последнего администратора района.
     */
    @Transactional
    public ChatUser updateCharacterSettings(Long characterId, CharacterUpdateRequest request) {
        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + characterId));

        if (request.role() != null) {
            boolean downgrade = character.getRole() == ChatRole.ADMIN && request.role() != ChatRole.ADMIN;
            if (downgrade) {
                long otherAdmins = chatUserRepository.findByDistrictId(character.getDistrictId()).stream()
                        .filter(u -> u.getRole() == ChatRole.ADMIN && !u.getId().equals(characterId))
                        .count();
                if (otherAdmins == 0) {
                    throw new IllegalStateException("Нельзя снять последнего администратора района");
                }
            }
            character.setRole(request.role());
        }
        if (request.profession() != null) {
            character.setProfession(request.profession());
        }
        if (request.gameStatus() != null) {
            character.setGameStatus(request.gameStatus());
        }
        if (request.coinBalance() != null) {
            character.setCoinBalance(request.coinBalance());
        }
        if (request.energy() != null) {
            character.setEnergy(request.energy());
        }
        if (request.rating() != null) {
            character.setRating(request.rating());
        }
        if (request.xpUser() != null) {
            character.setXpUser(request.xpUser());
        }
        return chatUserRepository.save(character);
    }

    /** Является ли персонаж (по username аккаунта) администратором. */
    public boolean isAdmin(String username) {
        return chatUserRepository.findByUsername(username)
                .map(u -> u.getRole() == ChatRole.ADMIN)
                .orElse(false);
    }

    /** Является ли персонаж администратором или модератором. */ //TODO  СМОТРЕТЬ НА location_members.posts
    public boolean isStaff(String username) {
        return chatUserRepository.findByUsername(username)
                .map(u -> u.getRole() == ChatRole.ADMIN || u.getRole() == ChatRole.MODERATOR)
                .orElse(false);
    }

    /** Аккаунт с «боевым» именем администратора автоматически получает роль ADMIN. */
    private boolean isAdminOwnerAccount(String username) {
        return username != null && username.equalsIgnoreCase("joks");
    }

    // создать персонажа для уже существующего User в указанном районе
    @Transactional
    public ChatUser createChatUser(Long userId, CreateChatUserRequest request) {
        // проверяем что User существует
        userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userId));

        // проверяем что у этого User ещё нет персонажа в данном районе
        if (chatUserRepository.existsByUserIdAndDistrictId(userId, request.getDistrictId())) {
            throw new RuntimeException(
                    "У пользователя уже есть персонаж в районе " + request.getDistrictId());
        }

        // проверяем уникальность имени персонажа в рамках района
        if (chatUserRepository.existsByCharacterNameAndDistrictId(
                request.getCharacterName(), request.getDistrictId())) {
            throw new RuntimeException(
                    "Имя персонажа уже занято в районе " + request.getDistrictId());
        }

        // получаем username из User
        String username = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден: " + userId))
                .getUsername();

        ChatUser chatUser = new ChatUser();
        chatUser.setUserId(userId);                 // связываем с User по userId
        chatUser.setUsername(username);             // дублируем для WS-уведомлений
        chatUser.setCharacterName(request.getCharacterName());
        chatUser.setDistrictId(request.getDistrictId());
        chatUser.setGender(request.getGender());
        chatUser.setWeight(request.getWeight());
        chatUser.setHeight(request.getHeight());
        chatUser.setBadHabits(request.getBadHabits());
        chatUser.setAvatarUrl(request.getAvatarUrl());
        chatUser.setRole(isAdminOwnerAccount(username) ? ChatRole.ADMIN : ChatRole.USER);  // по умолчанию USER, кроме аккаунта-админа
        chatUser.setRating(0);
        chatUser.setCoinBalance(0L);
        chatUser.setXpUser(0L);
        chatUser.setEnergy(100);
        chatUser.setGameStatus("HOMELESS");
        chatUser.setProfession(Profession.DVORNIK);
        chatUser.setLastSeen(LocalDateTime.now());
        chatUser.setEnergyLastUpdated(LocalDateTime.now());

        ChatUser saved = chatUserRepository.save(chatUser);

        // Автоматически добавляем персонажа во все системные локации района
        autoJoinSystemLocations(saved);

        return saved;
    }

    /**
     * Автоматически добавить персонажа во все системные локации района.
     * Вызывается при создании персонажа или при первом входе в район.
     * Если записи уже существуют — пропускает (идемпотентность).
     */
    private void autoJoinSystemLocations(ChatUser character) {
        List<Location> systemLocations = locationRepository
                .findByDistrictIdAndIsSystemTrue(character.getDistrictId());

        // Получаем ID системных локаций, в которых персонаж уже состоит
        Set<Long> existingLocationIds = locationMemberRepository
                .findByCharacterId(character.getId())
                .stream()
                .map(m -> m.getLocation().getId())
                .collect(Collectors.toSet());

        for (Location location : systemLocations) {
            if (!existingLocationIds.contains(location.getId())) {
                LocationMember member = new LocationMember();
                member.setLocation(location);
                member.setCharacter(character);
                member.setRole(LocationMemberRole.MEMBER);
                member.setIsRegistered(false);
                locationMemberRepository.save(member);
                log.info("Персонаж {} ({}) автоматически добавлен в системную локацию {}",
                        character.getCharacterName(), character.getId(), location.getName());
            }
        }
    }

    // топ персонажей по рейтингу
    public List<ChatUser> getTopChatUsers() {
        return chatUserRepository.findAllByOrderByRatingDesc();
    }

}