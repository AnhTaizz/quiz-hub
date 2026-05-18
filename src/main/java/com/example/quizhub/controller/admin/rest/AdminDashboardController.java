package com.example.quizhub.controller.admin.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.stream.Collectors;

import com.example.quizhub.dto.admin.response.AdminDashboardStatsResponse;
import com.example.quizhub.dto.admin.response.AdminReportStatsResponse;
import com.example.quizhub.dto.admin.response.RecentAttemptResponse;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.entity.enums.Role;
import com.example.quizhub.repository.AttemptRepository;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.QuizRepository;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.repository.PracticeRepository;
import com.example.quizhub.dto.admin.response.AdminDashboardDetailsResponse;
import java.time.format.DateTimeFormatter;
import java.util.*;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionRepository questionRepository;
    private final AttemptRepository attemptRepository;
    private final QuizRepository quizRepository;
    private final PracticeRepository practiceRepository;

    @GetMapping("/stats")
    public ResponseEntity<AdminDashboardStatsResponse> getStats() {
        long totalUsers = userRepository.count();
        long totalPublicCategories = categoryRepository.countByIsPublicTrue();
        long pendingQuestions = questionRepository.countByQuestionStatus(QuestionStatus.PENDING);
        long totalQuestions = questionRepository.count();

        return ResponseEntity.ok(new AdminDashboardStatsResponse(
                totalUsers, totalPublicCategories, pendingQuestions, totalQuestions
        ));
    }

    @GetMapping("/reports")
    public ResponseEntity<AdminReportStatsResponse> getReportStats() {
        AdminReportStatsResponse stats = AdminReportStatsResponse.builder()
                // User Distribution
                .studentCount(userRepository.countByRole(Role.STUDENT))
                .teacherCount(userRepository.countByRole(Role.TEACHER))
                .adminCount(userRepository.countByRole(Role.ADMIN))
                // Question Status
                .approvedQuestions(questionRepository.countByQuestionStatus(QuestionStatus.PUBLIC))
                .pendingQuestions(questionRepository.countByQuestionStatus(QuestionStatus.PENDING))
                .privateQuestions(questionRepository.countByQuestionStatus(QuestionStatus.PRIVATE))
                // Score Distribution
                .scoreLow(attemptRepository.countByResultLessThanAndEndedAtIsNotNull(new BigDecimal("5.0")))
                .scoreMedium(attemptRepository.countByResultBetweenAndEndedAtIsNotNull(new BigDecimal("5.0"), new BigDecimal("8.0")))
                .scoreHigh(attemptRepository.countByResultGreaterThanEqualAndEndedAtIsNotNull(new BigDecimal("8.0")))
                // Recent Activity (Merged Attempts & Practices)
                .recentAttempts(getCombinedRecentActivity())
                .build();

        return ResponseEntity.ok(stats);
    }

    private List<RecentAttemptResponse> getCombinedRecentActivity() {
        List<RecentAttemptResponse> activities = new ArrayList<>();

        // Add Recent Quiz Attempts
        attemptRepository.findTop10ByEndedAtIsNotNullOrderByStartedAtDesc().forEach(attempt -> {
            activities.add(RecentAttemptResponse.builder()
                    .id(attempt.getId())
                    .studentName(attempt.getQuizTaking().getLearner().getFullName())
                    .quizTitle(attempt.getQuizTaking().getQuiz().getTitle())
                    .score(attempt.getResult())
                    .startedAt(attempt.getStartedAt())
                    .type("QUIZ")
                    .build());
        });

        // Add Recent Practices
        practiceRepository.findTop10ByIsCompletedTrueOrderByCreatedAtDesc().forEach(p -> {
            BigDecimal score = BigDecimal.ZERO;
            if (p.getTotalQuestions() != null && p.getTotalQuestions() > 0) {
                double calc = (p.getCorrectAnswers() * 10.0) / p.getTotalQuestions();
                score = BigDecimal.valueOf(calc).setScale(1, java.math.RoundingMode.HALF_UP);
            }

            activities.add(RecentAttemptResponse.builder()
                    .id(p.getId())
                    .studentName(p.getUser().getFullName())
                    .quizTitle("Luyện tập: " + (p.getCategory() != null ? p.getCategory().getName() : "N/A"))
                    .score(score)
                    .startedAt(p.getCreatedAt())
                    .type("PRACTICE")
                    .build());
        });

        // Sort by date desc and limit to 10
        return activities.stream()
                .sorted(Comparator.comparing(RecentAttemptResponse::getStartedAt).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    @GetMapping("/details")
    public ResponseEntity<AdminDashboardDetailsResponse> getDashboardDetails() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        AdminDashboardDetailsResponse details = AdminDashboardDetailsResponse.builder()
                // Recent Users
                .recentUsers(userRepository.findTop5ByOrderByCreatedAtDesc().stream()
                        .map(u -> AdminDashboardDetailsResponse.DashboardUserResponse.builder()
                                .fullName(u.getFullName())
                                .email(u.getEmail())
                                .role(u.getRole().name())
                                .createdAt(u.getCreatedAt() != null ? u.getCreatedAt().format(formatter) : "N/A")
                                .build())
                        .collect(Collectors.toList()))
                // Pending Moderation (Questions only for now)
                .pendingModeration(questionRepository.findTop5ByQuestionStatusOrderByIdDesc(QuestionStatus.PENDING).stream()
                        .map(q -> AdminDashboardDetailsResponse.DashboardModerationResponse.builder()
                                .id(q.getId())
                                .text(q.getText())
                                .type("Câu hỏi")
                                .creatorName(q.getCreator() != null ? q.getCreator().getFullName() : "N/A")
                                .categoryName(q.getCategory() != null ? q.getCategory().getName() : "N/A")
                                .build())
                        .collect(Collectors.toList()))
                // Categories
                .categories(categoryRepository.findTop5ByIsPublicTrueOrderByIdDesc().stream()
                        .map(c -> AdminDashboardDetailsResponse.DashboardCategoryResponse.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .quizCount(quizRepository.countByCategoryId(c.getId()))
                                .questionCount(questionRepository.countByCategoryId(c.getId()))
                                .build())
                        .collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(details);
    }
}
