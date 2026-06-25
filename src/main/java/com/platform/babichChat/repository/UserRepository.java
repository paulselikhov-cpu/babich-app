package com.platform.babichChat.repository;

import com.platform.babichChat.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // JpaRepository уже содержит:
    // findAll() — все пользователи
    // findById(id) — по id
    // save(user) — создать/обновить
    // deleteById(id) — удалить
    // Ничего писать не нужно!
}