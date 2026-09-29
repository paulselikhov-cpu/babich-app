package com.platform.chat.features.application.dto;

import com.platform.auth.service.UserService;
import com.platform.chat.engines.application.dto.ApplicationSummary;
import com.platform.chat.engines.application.enums.ApplicationStatus;
import com.platform.chat.engines.application.enums.ApplicationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Сборка DTO модерации: имя заявителя приходит одним батч-вызовом публичного API
 * модуля auth ({@code UserService.getUsernamesByIds}) — без N+1 и без доступа
 * в чужой репозиторий.
 */
@ExtendWith(MockitoExtension.class)
class ApplicationViewAssemblerTest {

    @Mock
    private UserService userService;

    private ApplicationViewAssembler assembler;

    @BeforeEach
    void setUp() {
        assembler = new ApplicationViewAssembler(userService);
    }

    private ApplicationSummary summary(Long id, Long userId) {
        return new ApplicationSummary(id, ApplicationType.REGISTER_PASSPORT, ApplicationStatus.PENDING,
                null, LocalDateTime.now(), null, userId, 7L, null);
    }

    @Test
    void fillsApplicantUsernameFromBatchLookup() {
        List<ApplicationSummary> applications = List.of(summary(1L, 100L), summary(2L, 200L));
        when(userService.getUsernamesByIds(ArgumentMatchers.<Long>anyCollection()))
                .thenReturn(Map.of(100L, "ivan", 200L, "petr"));

        List<ApplicationView> views = assembler.assemble(applications);

        assertThat(views).hasSize(2);
        assertThat(views).extracting(ApplicationView::id).containsExactly(1L, 2L);
        assertThat(views).extracting(ApplicationView::applicantUsername).containsExactly("ivan", "petr");
        // один вызов на всю пачку — ради этого ассемблер и появился
        verify(userService).getUsernamesByIds(ArgumentMatchers.<Long>anyCollection());
    }

    @Test
    void unknownUserLeavesUsernameEmpty() {
        when(userService.getUsernamesByIds(ArgumentMatchers.<Long>anyCollection())).thenReturn(Map.of());

        List<ApplicationView> views = assembler.assemble(List.of(summary(1L, 999L)));

        assertThat(views).singleElement().satisfies(view -> {
            assertThat(view.userId()).isEqualTo(999L);
            assertThat(view.applicantUsername()).isNull();
        });
    }

    @Test
    void emptyInputDoesNotTouchUserService() {
        assertThat(assembler.assemble(List.of())).isEmpty();
        verifyNoInteractions(userService);
    }
}
