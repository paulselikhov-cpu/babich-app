package com.platform.chat.base.location.service;

import com.platform.chat.base.chatUser.entity.ChatUser;
import com.platform.chat.base.district.entity.District;
import com.platform.chat.base.location.dto.LocationResponse;
import com.platform.chat.base.location.enums.LocationUserRole;
import com.platform.chat.base.chatUser.repository.ChatUserRepository;
import com.platform.chat.base.district.repository.DistrictRepository;
import com.platform.chat.base.location.entity.Location;
import com.platform.chat.base.location.entity.LocationTemplate;
import com.platform.chat.base.location.entity.LocationUser;
import com.platform.chat.base.location.repository.LocationUserRepository;
import com.platform.chat.base.location.repository.LocationRepository;
import com.platform.chat.base.location.repository.LocationTemplateRepository;
import com.platform.chat.base.room.entity.Room;
import com.platform.chat.base.room.repository.RoomReadStatusRepository;
import com.platform.chat.base.room.repository.RoomRepository;
import com.platform.chat.core.modules.transactionLog.enums.CoinUpdateReason;
import com.platform.chat.modules.coin.service.CoinService;
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
    private final LocationUserRepository locationUserRepository;
    private final RoomReadStatusRepository roomReadStatusRepository;
    private final RoomRepository roomRepository;
    private final ChatUserRepository chatUserRepository;
    private final DistrictRepository districtRepository;
    private final CoinService coinService;

    /**
     * Получить все публичные пользовательские локации.
     * Показываем в общем списке — "найти локацию".
     */
    public List<Location> getPublicLocations() {
        return locationRepository.findByIsPublicTrue();
    }

    /**
     * Получить все системные локации.
     */
    public List<Location> getSystemLocations() {
        return locationRepository.findByIsSystemTrue();
    }

    /**
     * Получить локации пользователя с его ролью в каждой.
     * Для системной локации membership = штат (держатель должности),
     * поэтому она попадает в «мои локации» только если персонаж в ней что-то занимает.
     * Сами публичные системные локации фронт берёт из /api/districts (не зависят от членства).
     */
    public List<LocationResponse> getMyLocations(Long characterId) {
        List<LocationUser> memberships = locationUserRepository
                .findByCharacterId(characterId);

        return memberships.stream()
                .map(member -> LocationResponse.from(
                        member.getLocation(),
                        member.getRole()
                ))
                .collect(java.util.stream.Collectors.toList());
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
     * 6. Добавляем создателя как OWNER в location_users
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
        }

        // шаг 4: получаем дефолтный район (персонаж всегда живёт в каком-то районе)
        District defaultDistrict = districtRepository.findByCode("CENTRAL")
                .orElseThrow(() -> new RuntimeException("Дефолтный район не найден"));

        // шаг 5: создаём локацию
        Location location = new Location();
        location.setTemplate(template);
        location.setOwner(character);
        location.setDistrict(defaultDistrict);
        location.setName(name);
        location.setDescription(description);
        location.setIsPublic(isPublic);
        location.setInviteCode(generateUniqueInviteCode());

        Location savedLocation = locationRepository.save(location);

        // шаг 3b: списываем Coin за создание (аудит — через доменное событие)
        if (template.getPriceCoin() > 0) {
            coinService.takeCoins(characterId, template.getPriceCoin(),
                    CoinUpdateReason.LOCATION_CREATE, savedLocation.getId());
        }

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
        LocationUser ownerMember = new LocationUser();
        ownerMember.setLocation(savedLocation);
        ownerMember.setCharacter(character);
        ownerMember.setRole(LocationUserRole.OWNER);
        ownerMember.setIsRegistered(true); // владелец всегда "прописан"
        locationUserRepository.save(ownerMember);

        return savedLocation;
    }

    /**
     * Вступить в локацию по invite_code.
     * Вызывается когда пользователь перешёл по ссылке-приглашению.
     *
     * Что происходит:
     * 1. Находим локацию по коду
     * 2. Проверяем что персонаж ещё не в локации
     * 3. Добавляем запись в location_users
     */
    @Transactional
    public LocationUser joinByInviteCode(String inviteCode, Long characterId) {
        Location location = locationRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new RuntimeException("Локация не найдена"));

        // если уже состоит — возвращаем существующее членство вместо ошибки
        Optional<LocationUser> existing = locationUserRepository
                .findByLocationIdAndCharacterId(location.getId(), characterId);

        if (existing.isPresent()) {
            return existing.get();
        }

        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + characterId));

        LocationUser member = new LocationUser();
        member.setLocation(location);
        member.setCharacter(character);
        member.setRole(LocationUserRole.MEMBER);
        member.setIsRegistered(false);

        return locationUserRepository.save(member);
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
        LocationUser member = locationUserRepository
                .findByLocationIdAndCharacterId(locationId, characterId)
                .orElseThrow(() -> new RuntimeException("Вы не состоите в этой локации"));

        if (member.getRole() != LocationUserRole.OWNER) {
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

        // проверяем баланс и списываем Coin через CoinService
        Long price = template.getExtendPriceCoin();
        if (price != null && price > 0) {
            ChatUser character = chatUserRepository.findById(characterId)
                    .orElseThrow(() -> new RuntimeException(
                            "Персонаж не найден"
                    ));

            if (character.getCoinBalance() < price) {
                throw new RuntimeException(
                        "Недостаточно Coin. Нужно: " + price +
                                ", есть: " + character.getCoinBalance()
                );
            }
            coinService.takeCoins(characterId, price,
                    CoinUpdateReason.LOCATION_EXTEND_ROOM, locationId);
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
    public LocationUser appointModerator(Long locationId,
                                           Long ownerId,
                                           Long targetCharacterId) {
        // проверяем что это владелец
        LocationUser ownerMember = locationUserRepository
                .findByLocationIdAndCharacterId(locationId, ownerId)
                .orElseThrow(() -> new RuntimeException(
                        "Вы не состоите в этой локации"
                ));

        if (ownerMember.getRole() != LocationUserRole.OWNER) {
            throw new RuntimeException(
                    "Только владелец может назначать модераторов"
            );
        }

        // находим целевого участника
        LocationUser targetMember = locationUserRepository
                .findByLocationIdAndCharacterId(locationId, targetCharacterId)
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж не состоит в этой локации"
                ));

        // "прописываем" — назначаем модератором
        targetMember.setRole(LocationUserRole.MODERATOR);
        targetMember.setIsRegistered(true);

        return locationUserRepository.save(targetMember);
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

    /**
     * Покинуть локацию (для участника/модератора).
     */
    @Transactional
    public void leaveLocation(Long locationId, Long characterId) {
        LocationUser member = locationUserRepository
                .findByLocationIdAndCharacterId(locationId, characterId)
                .orElseThrow(() -> new RuntimeException("Вы не состоите в этой локации"));

        if (member.getRole() == LocationUserRole.OWNER) {
            throw new RuntimeException("Владелец не может покинуть локацию — только удалить");
        }

        locationUserRepository.delete(member);
    }

    /**
     * Удалить локацию (только для владельца).
     * Удаляет всё — комнаты, сообщения, участников.
     */
    @Transactional
    public void deleteLocation(Long locationId, Long characterId) {
        LocationUser member = locationUserRepository
                .findByLocationIdAndCharacterId(locationId, characterId)
                .orElseThrow(() -> new RuntimeException("Вы не состоите в этой локации"));

        if (member.getRole() != LocationUserRole.OWNER) {
            throw new RuntimeException("Только владелец может удалить локацию");
        }

        // 1. Получаем все комнаты локации
        List<Room> rooms = roomRepository.findByLocationIdOrderByOrderIndex(locationId);
        List<Long> roomIds = rooms.stream().map(Room::getId).toList();

        // 2. Удаляем room_read_status для всех комнат (иначе FK нарушается)
        if (!roomIds.isEmpty()) {
            roomReadStatusRepository.deleteByRoomIdIn(roomIds);
        }

        // 3. Удаляем сообщения комнат (у них cascade = ALL на Room)
        roomRepository.deleteAll(rooms);

        // 4. Удаляем участников локации
        List<LocationUser> members = locationUserRepository.findByLocationId(locationId);
        if (!members.isEmpty()) {
            locationUserRepository.deleteAll(members);
        }

        // 5. Удаляем саму локацию
        locationRepository.deleteById(locationId);
    }
}