package com.platform.chat.core.modules.application.repository;

import com.platform.chat.core.modules.application.entity.Application;
import com.platform.chat.core.modules.application.models.ApplicationStatus;
import com.platform.chat.core.modules.application.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Все заявки, свежие сверху — для модерации («Гражданские заявки»). */
    List<Application> findAllByOrderByCreatedAtDesc();

    /** Заявки на получение паспорта, свежие сверху. */
    List<Application> findByTypeOrderByCreatedAtDesc(ApplicationType type);

    boolean existsByUserIdAndTypeAndStatus(Long userId, ApplicationType type, ApplicationStatus status);

    /** Есть ли незакрытая заявка ТИПА у конкретного персонажа (адресно). */
    boolean existsByCharacterIdAndTypeAndStatus(Long characterId, ApplicationType type, ApplicationStatus status);
}
