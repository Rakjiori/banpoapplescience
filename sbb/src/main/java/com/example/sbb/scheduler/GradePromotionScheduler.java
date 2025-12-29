package com.example.sbb.scheduler;

import com.example.sbb.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GradePromotionScheduler {

    private final UserService userService;

    // 매년 1월 1일 04:00에 학년 자동 진급 (예비1→1→2→3→졸업생)
    @Scheduled(cron = "0 0 4 1 1 *")
    public void promoteYearly() {
        userService.promoteStudentGrades();
    }
}
