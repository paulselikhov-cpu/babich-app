package com.platform.chat.modules.civic.repository;

import com.platform.chat.modules.civic.entity.Application;
import com.platform.chat.modules.civic.enums.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Все заявки, свежие сверху — для модерации («Гражданские заявки»). */
    List<Application> findAllByOrderByCreatedAtDesc();

    boolean existsByUserIdAndType(Long userId, ApplicationType type);
}
