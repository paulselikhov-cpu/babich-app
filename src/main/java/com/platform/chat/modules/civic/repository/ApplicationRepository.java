package com.platform.civic.repository;

import com.platform.civic.entity.Application;
import com.platform.civic.enums.ApplicationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndType(Long userId, ApplicationType type);
}
