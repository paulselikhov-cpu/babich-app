package com.platform.chat.service.location;

import com.platform.chat.dto.request.LocationPostViewRequest;
import com.platform.chat.entity.ChatUser;
import com.platform.chat.entity.Location;
import com.platform.chat.entity.LocationPost;
import com.platform.chat.enums.LocationPostType;
import com.platform.chat.repository.chatUser.ChatUserRepository;
import com.platform.chat.repository.location.LocationPostRepository;
import com.platform.chat.repository.location.LocationRepository;
import com.platform.chat.repository.location.LocationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Сервис для управления должностями в системных локациях.
 */
@Service
@RequiredArgsConstructor
public class LocationPostService {

    private final LocationPostRepository locationPostRepository;
    private final LocationRepository locationRepository;
    private final ChatUserRepository chatUserRepository;
    private final LocationMemberRepository locationMemberRepository;

    /**
     * Назначить персонажа на должность в системной локации.
     * Семантика согласована с ограничениями таблицы location_posts:
     * - unique(location_id, character_id) — персонаж занимает одну должность
     *   в локации; повторный вызов для той же должности идемпотентен
     *   (возвращает существующую запись);
     * - unique(location_id, post) — в локации каждый тип должности
     *   одноместный; если должность занята другим — бизнес-ошибка 400.
     */
    @Transactional
    public LocationPost appoint(Long locationId, Long characterId, LocationPostType post) {
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("Локация не найдена: " + locationId));

        if (!location.getIsSystem()) {
            throw new RuntimeException("Должности можно назначать только в системных локациях");
        }

        ChatUser character = chatUserRepository.findById(characterId)
                .orElseThrow(() -> new RuntimeException("Персонаж не найден: " + characterId));

        // Проверяем, что персонаж уже состоит в локации (авто-членство или вступление)
        locationMemberRepository.findByLocationIdAndCharacterId(locationId, characterId)
                .orElseThrow(() -> new RuntimeException(
                        "Персонаж " + characterId + " не состоит в локации " + locationId));

        // Персонаж уже занимает должность в этой локации?
        Optional<LocationPost> mine =
                locationPostRepository.findByLocationIdAndCharacterId(locationId, characterId);
        if (mine.isPresent()) {
            if (mine.get().getPost() == post) {
                return mine.get(); // уже назначен на эту же должность — идемпотентно
            }
            throw new RuntimeException(
                    "Персонаж уже занимает должность " + mine.get().getPost()
                            + " в этой локации — нельзя занять ещё и " + post);
        }

        // Должность уже занята другим персонажем? (все должности одноместные)
        locationPostRepository.findByLocationIdAndPost(locationId, post)
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "Должность " + post + " в локации " + locationId + " уже занята");
                });

        LocationPost locationPost = new LocationPost();
        locationPost.setLocation(location);
        locationPost.setCharacter(character);
        locationPost.setPost(post);

        return locationPostRepository.save(locationPost);
    }

    /**
     * Снять персонажа с должности.
     */
    @Transactional
    public void dismiss(Long postId) {
        LocationPost locationPost = locationPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Должность не найдена: " + postId));
        locationPostRepository.delete(locationPost);
    }

    /**
     * Получить все должности в системной локации.
     */
    @Transactional(readOnly = true)
    public List<LocationPost> getPostsByLocation(Long locationId) {
        return locationPostRepository.findByLocationId(locationId);
    }

    /**
     * Должности в локации вместе с именем персонажа (для UI).
     */
    @Transactional(readOnly = true)
    public List<LocationPostViewRequest> getPostViewsByLocation(Long locationId) {
        return locationPostRepository.findByLocationId(locationId).stream()
                .map(lp -> new LocationPostViewRequest(
                        lp.getId(),
                        lp.getCharacter().getId(),
                        lp.getCharacter().getCharacterName(),
                        lp.getPost(),
                        lp.getAppointedAt()))
                .toList();
    }

    /**
     * Получить все должности персонажа.
     */
    @Transactional(readOnly = true)
    public List<LocationPost> getPostsByCharacter(Long characterId) {
        return locationPostRepository.findByCharacterId(characterId);
    }
}