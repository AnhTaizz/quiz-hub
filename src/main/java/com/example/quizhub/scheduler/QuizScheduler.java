package com.example.quizhub.scheduler;

import com.example.quizhub.service.quiztaking.QuizTakingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class QuizScheduler {

    private final QuizTakingService quizTakingService;

    // Chạy mỗi phút để kiểm tra các bài thi hết hạn
    @Scheduled(cron = "0 * * * * *")
    public void autoSubmitExpiredQuizzes() {
        log.info("Starting auto-submit task for expired quizzes...");
        try {
            quizTakingService.autoSubmitExpiredAttempts();
        } catch (Exception e) {
            log.error("Error during auto-submit task: ", e);
        }
    }
}
