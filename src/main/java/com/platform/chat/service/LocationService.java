package com.platform.chat.service;

import com.platform.chat.entity.*;
import com.platform.chat.enums.LocationMemberRole;
import com.platform.chat.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для работы с локациями.
 * Локация = экземпляр созданный пользователем из шаблона.
 *
 * Основные сценарии:
 * 1. Пользователь выбирает шаблон → создаём локацию с базовыми комнатами
 * 2. Пользователь делится ссылкой → другой вступает по invite_code
 * 3. Владелец управляет локацией — добавляет комнаты, назначает модераторов
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;
    private final LocationTemplateRepository locationTemplateRepository;
    private final LocationMemberRepository locationMemberRepository;
    private final RoomRepository roomRepository;
    private final ChatUserRepository chatUserRepository;

    /**
     * Получить все публичные локации.
     * Показываем в общем списке — "найти локацию".
     */
    public List<Location> getPublicLocations() {
        return locationRepository.findByIsPublicTrue();
    }

    /**
     * Получить все локации персонажа (где он участник).
     * Показываем в боковом меню — "мои локации".
     */
    public List<Location> getMyLocations(Long characterId) {
        return locationRepository.findAllByMemberCharacterId(characterId);
    }

    /**
     * Получить локации которыми владеет персонаж.
     * Показываем в профиле — "мои созданные локации".
     */
    public List<Location> getOwnedLocations(Long ownerId) {
        return locationRepository.findByOwnerId(ownerId);
    }

    /**
     * Создать новую локацию из шаблона.
     *
     * Что происходит при создании:
     * 1. Проверяем что шаблон существует
     * 2. Если шаблон платный — проверяем баланс Coin персонажа
     * 3. Создаём локацию
     * 4. Генерируем уникальный invite_code
     * 5. Копируем базовые комнаты из шаблона
     * 6. Добавляем создателя как OWNER в location_members
     *
     * @Transactional — всё создаётся за одну транзакцию.
     * Если что-то упало — откатываем всё назад.
     */
    @Transactional
    public Location createLocation(Long templateId,
                                   Long characterId,
                                   String name,
                                   String description,
                                   Boolean isPublic) {
        // шаг 1: получаем шаблон
        LocationTemplate template = locationTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException(
                        "Шаблон не найден: " + templateId
                ));

        // шаг 2: получаем персонажа
        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж не найден: " + characterId
                ));

        // шаг 3: если шаблон платный — проверяем баланс
        if (template.getPriceCoin() > 0) {
            if (character.getCoinBalance() < template.getPriceCoin()) {
                throw new RuntimeException(
                        "Недостаточно Coin. Нужно: " + template.getPriceCoin() +
                                ", есть: " + character.getCoinBalance()
                );
            }
            // списываем Coin
            character.setCoinBalance(
                    character.getCoinBalance() - template.getPriceCoin()
            );
            chatUserRepository.save(character);
        }

        // шаг 4: создаём локацию
        Location location = new Location();
        location.setTemplate(template);
        location.setOwner(character);
        location.setName(name);
        location.setDescription(description);
        location.setIsPublic(isPublic);
        location.setInviteCode(generateUniqueInviteCode());

        Location savedLocation = locationRepository.save(location);

        // шаг 5: копируем базовые комнаты из шаблона
        // каждая TemplateRoom → новая Room в этой локации
        if (template.getTemplateRooms() != null) {
            template.getTemplateRooms().forEach(templateRoom -> {
                Room room = new Room();
                room.setLocation(savedLocation);
                room.setName(templateRoom.getName());
                room.setDescription(templateRoom.getDescription());
                room.setOrderIndex(templateRoom.getOrderIndex());
                room.setIsCustom(false);   // базовая комната из шаблона
                room.setStoreHistory(true); // по умолчанию храним историю
                roomRepository.save(room);
            });
        }

        // шаг 6: добавляем создателя как OWNER
        LocationMember ownerMember = new LocationMember();
        ownerMember.setLocation(savedLocation);
        ownerMember.setCharacter(character);
        ownerMember.setRole(LocationMemberRole.OWNER);
        ownerMember.setIsRegistered(true); // владелец всегда "прописан"
        locationMemberRepository.save(ownerMember);

        return savedLocation;
    }

    /**
     * Вступить в локацию по invite_code.
     * Вызывается когда пользователь перешёл по ссылке-приглашению.
     *
     * Что происходит:
     * 1. Находим локацию по коду
     * 2. Проверяем что персонаж ещё не в локации
     * 3. Добавляем запись в location_members
     */
    @Transactional
    public LocationMember joinByInviteCode(String inviteCode, Long characterId) {
        Location location = locationRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Локация не найдена"));

        // если уже состоит — возвращаем существующее членство вместо ошибки
        Optional<LocationMember> existing = locationMemberRepository
                .findByLocationIdAndCharacterId(location.getId(), characterId);

        if (existing.isPresent()) {
            return existing.get();
        }

        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + characterId));

        LocationMember member = new LocationMember();
        member.setLocation(location);
        member.setCharacter(character);
        member.setRole(LocationMemberRole.MEMBER);
        member.setIsRegistered(false);

        return locationMemberRepository.save(member);
    }

    /**
     * Добавить кастомную комнату в расширяемую локацию.
     * Только владелец может добавлять комнаты.
     * Только если шаблон локации isExtendable = true.
     * Стоит extendPriceCoin Coin.
     */
    @Transactional
    public Room addCustomRoom(Long locationId,
                              Long characterId,
                              String roomName,
                              String roomDescription) {
        // получаем локацию
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException(
                        "Локация не найдена: " + locationId
                ));

        // проверяем что это владелец
        LocationMember member = locationMemberRepository
                .findByLocationIdAndCharacterId(locationId, characterId)
                .orElseThrow(() -> new RuntimeException("Вы не состоите в этой локации"));

        if (member.getRole() != LocationMemberRole.OWNER) {
            throw new RuntimeException(
                    "Только владелец может добавлять комнаты"
            );
        }

        // проверяем что шаблон расширяемый
        LocationTemplate template = location.getTemplate();
        if (!template.getIsExtendable()) {
            throw new RuntimeException(
                    "Этот тип локации нельзя расширять"
            );
        }

        // проверяем баланс и списываем Coin
        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж не найден"
                ));

        Long price = template.getExtendPriceCoin();
        if (price != null && price > 0) {
            if (character.getCoinBalance() < price) {
                throw new RuntimeException(
                        "Недостаточно Coin. Нужно: " + price +
                                ", есть: " + character.getCoinBalance()
                );
            }
            character.setCoinBalance(character.getCoinBalance() - price);
            chatUserRepository.save(character);
        }

        // определяем порядковый номер новой комнаты
        long roomCount = roomRepository.countByLocationId(locationId);

        // создаём комнату
        Room room = new Room();
        room.setLocation(location);
        room.setName(roomName);
        room.setDescription(roomDescription);
        room.setIsCustom(true);   // помечаем что добавлена вручную
        room.setStoreHistory(true);
        room.setOrderIndex((int) roomCount + 1);

        return roomRepository.save(room);
    }

    /**
     * Назначить модератора ("прописать") в локации.
     * Только владелец может назначать модераторов.
     */
    @Transactional
    public LocationMember appointModerator(Long locationId,
                                           Long ownerId,
                                           Long targetCharacterId) {
        // проверяем что это владелец
        LocationMember ownerMember = locationMemberRepository
                .findByLocationIdAndCharacterId(locationId, ownerId)
                .orElseThrow(() -> new RuntimeException(
                        "Вы не состоите в этой локации"
                ));

        if (ownerMember.getRole() != LocationMemberRole.OWNER) {
            throw new RuntimeException(
                    "Только владелец может назначать модераторов"
            );
        }

        // находим целевого участника
        LocationMember targetMember = locationMemberRepository
                .findByLocationIdAndCharacterId(locationId, targetCharacterId)
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж не состоит в этой локации"
                ));

        // "прописываем" — назначаем модератором
        targetMember.setRole(LocationMemberRole.MODERATOR);
        targetMember.setIsRegistered(true);

        return locationMemberRepository.save(targetMember);
    }

    /**
     * Обновить настройку хранения истории для комнаты.
     * Только владелец или модератор может менять настройки.
     */
    @Transactional
    public Room updateRoomStoreHistory(Long roomId,
                                       Long characterId,
                                       Boolean storeHistory) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException(
                        "Комната не найдена: " + roomId
                ));

        // проверяем права — владелец или модератор локации
        LocationMember member = locationMemberRepository
                .findByLocationIdAndCharacterId(
                        room.getLocation().getId(), characterId
                )
                .orElseThrow(() -> new RuntimeException(
                        "Вы не состоите в этой локации"
                ));

        if (member.getRole() == LocationMemberRole.MEMBER) {
            throw new RuntimeException(
                    "Недостаточно прав для изменения настроек комнаты"
            );
        }

        room.setStoreHistory(storeHistory);
        return roomRepository.save(room);
    }

    private String generateUniqueInviteCode() {
        String code;
        do {
            code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 8);
        } while (locationRepository.existsByInviteCode(code));
        return code;
    }
}