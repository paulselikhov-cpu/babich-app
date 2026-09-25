package com.platform.chat.core.modules.application.repository;

import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.enums.ApplicationStatus;
import com.platform.chat.core.modules.application.enums.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Все заявки, свежие сверху. */
    List<Application> findAllByOrderByCreatedAtDesc();

    /** Есть ли незакрытая заявка ТИПА у конкретного персонажа (адресно). */
    boolean existsByCharacterIdAndTypeAndStatus(Long characterId, ApplicationType type, ApplicationStatus status);
}
