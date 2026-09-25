package com.platform.auth.service;

import com.platform.auth.entity.User;
import com.platform.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.NamedInterface;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@NamedInterface("userService")
@Service
@RequiredArgsConstructor
/**
 * Пользователи аккаунтов. Помечен {@link NamedInterface}: это публичный API
 * модуля {@code auth} для чтения пользователей — другие модули ходят за именем
 * пользователя через него, а не в {@code UserRepository} (модуль {@code auth}
 * объявлен {@code CLOSED} и наружу отдаёт только {@code entityUser},
 * {@code userRepository} и {@code userService}).
 */
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Имена пользователей по пачке id — одним запросом.
     * Нужен тем, кто собирает DTO с именем владельца/заявителя: вызов
     * {@link #getUserById(Long)} в цикле дал бы N+1 запросов.
     *
     * @return {@code id → username}; для неизвестных id записи нет
     */
    public Map<Long, String> getUsernamesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
    }

    public User createUser(User user) {
        // createdAt will be set automatically by @PrePersist
        return userRepository.save(user);
    }

    public Optional<User> updateUser(Long id, User user) {
        return userRepository.findById(id)
                .map(existingUser -> {
                    existingUser.setUsername(user.getUsername());
                    existingUser.setPassword(user.getPassword());
                    existingUser.setEmail(user.getEmail());
                    // createdAt is not updated (keeps original creation time)
                    return userRepository.save(existingUser);
                });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}