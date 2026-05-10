package com.example.quizhub.controller.student;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.quizhub.entity.User;
import com.example.quizhub.repository.ClassJoiningRepository;
import com.example.quizhub.repository.QuizAssigningRepository;
import com.example.quizhub.entity.QuizAssigning;
import com.example.quizhub.entity.ClassJoining;
import com.example.quizhub.entity.Practice;
import com.example.quizhub.entity.Attempt;
import com.example.quizhub.entity.QuizTaking;
import com.example.quizhub.entity.enums.JoinStatus;
import com.example.quizhub.entity.enums.TakingStatus;
import com.example.quizhub.repository.AttemptRepository;
import com.example.quizhub.repository.PracticeRepository;
import com.example.quizhub.repository.QuizTakingRepository;
import com.example.quizhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
@Slf4j
public class StudentHomeController {

    private final AttemptRepository attemptRepository;
    private final PracticeRepository practiceRepository;
    private final QuizTakingRepository quizTakingRepository;
    private final UserRepository userRepository;
    private final ClassJoiningRepository classJoiningRepository;
    private final QuizAssigningRepository quizAssigningRepository;

    @GetMapping
    public String home(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByEmail(email).orElse(null);

        if (student != null) {
            List<Attempt> allCompletedAttempts = attemptRepository
                    .findByQuizTakingLearnerIdAndEndedAtIsNotNull(student.getId());
            List<Attempt> classroomAttempts = allCompletedAttempts.stream()
                    .filter(a -> a.getQuizTaking() != null && a.getQuizTaking().getQuizAssigning() != null)
                    .collect(Collectors.toList());

            long attemptCount = classroomAttempts.size();
            long practiceCount = practiceRepository.countByUserIdAndIsCompletedTrue(student.getId());
            long totalCompleted = attemptCount + practiceCount;

            List<Practice> completedPractices = practiceRepository
                    .findByUserIdAndIsCompletedTrueOrderByCreatedAtDesc(student.getId());

            BigDecimal quizAvg = BigDecimal.ZERO;
            if (!classroomAttempts.isEmpty()) {
                BigDecimal sum = classroomAttempts.stream()
                        .map(a -> a.getResult() != null ? a.getResult() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                quizAvg = sum.divide(BigDecimal.valueOf(classroomAttempts.size()), 1, RoundingMode.HALF_UP);
            }

            BigDecimal practiceAvg = BigDecimal.ZERO;
            if (!completedPractices.isEmpty()) {
                BigDecimal sum = completedPractices.stream()
                        .map(p -> {
                            if (p.getTotalQuestions() != null && p.getTotalQuestions() > 0) {
                                double calc = (p.getCorrectAnswers() * 10.0) / p.getTotalQuestions();
                                return BigDecimal.valueOf(calc);
                            }
                            return BigDecimal.ZERO;
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                practiceAvg = sum.divide(BigDecimal.valueOf(completedPractices.size()), 1, RoundingMode.HALF_UP);
            }

            // Get all assigned quizzes from joined classrooms
            List<ClassJoining> joinedClasses = classJoiningRepository.findByLearnerIdAndStatusIn(
                    student.getId(),
                    List.of(JoinStatus.APPROVED, JoinStatus.PENDING));
            List<QuizAssigning> rawAssignedQuizzes = new ArrayList<>();
            for (ClassJoining joining : joinedClasses) {
                if (joining.getStatus() == JoinStatus.APPROVED) {
                    rawAssignedQuizzes
                            .addAll(quizAssigningRepository.findByClassroomId(joining.getClassroom().getId()));
                }
            }

            // Filter and map to dashboard-friendly structure
            List<QuizDashboardInfo> dashboardQuizzes = new ArrayList<>();
            int pendingThisWeekCount = 0;
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextWeek = now.plusDays(7);

            for (QuizAssigning assigning : rawAssignedQuizzes) {
                QuizTaking taking = quizTakingRepository
                        .findByLearnerIdAndQuizAssigningId(student.getId(), assigning.getId())
                        .orElse(null);

                int finishedCount = 0;
                boolean anyAttemptExists = false;
                boolean hasUnfinished = false;
                if (taking != null) {
                    List<Attempt> attempts = attemptRepository.findByQuizTakingId(taking.getId());
                    finishedCount = (int) attempts.stream().filter(a -> a.getEndedAt() != null).count();
                    anyAttemptExists = !attempts.isEmpty();
                    hasUnfinished = attempts.stream().anyMatch(a -> a.getEndedAt() == null);
                }

                int max = assigning.getMaxAttempt() != null ? assigning.getMaxAttempt() : 0;
                boolean hasAttemptsLeft = (max == 0) || (finishedCount < max);

                boolean isExpired = assigning.getDueDate() != null && now.isAfter(assigning.getDueDate());
                boolean isUpcoming = assigning.getStartDate() != null && now.isBefore(assigning.getStartDate());

                if (hasAttemptsLeft && !isExpired && !isUpcoming) {
                    QuizDashboardInfo info = new QuizDashboardInfo();
                    info.setAssigning(assigning);
                    info.setAttemptsMade(finishedCount);
                    info.setAttemptsLeft(max == 0 ? -1 : (max - finishedCount));
                    info.setHasStarted(anyAttemptExists);
                    info.setHasUnfinished(hasUnfinished);
                    dashboardQuizzes.add(info);

                    // Count for 'this week' message
                    if (assigning.getDueDate() != null && assigning.getDueDate().isBefore(nextWeek)) {
                        pendingThisWeekCount++;
                    }
                }
            }

            model.addAttribute("totalCompleted", totalCompleted);
            model.addAttribute("quizAvg", quizAvg);
            model.addAttribute("practiceAvg", practiceAvg);
            model.addAttribute("assignedQuizzes", dashboardQuizzes);
            model.addAttribute("pendingCount", dashboardQuizzes.size());
            model.addAttribute("pendingThisWeekCount", pendingThisWeekCount);
            model.addAttribute("greeting", getGreeting());
        }

        return "student/student-home";
    }

    @GetMapping("/quizzes")
    public String listAllQuizzes(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByEmail(email).orElse(null);

        if (student != null) {
            // Get all classrooms to match home() logic
            List<ClassJoining> joinedClasses = classJoiningRepository.findByLearnerIdAndStatusIn(
                    student.getId(),
                    List.of(JoinStatus.APPROVED, JoinStatus.PENDING));

            Map<Long, QuizDashboardInfo> quizMap = new LinkedHashMap<>();
            LocalDateTime now = LocalDateTime.now();

            for (ClassJoining joining : joinedClasses) {
                if (joining.getStatus() == JoinStatus.APPROVED && joining.getClassroom() != null) {
                    Long classId = joining.getClassroom().getId();
                    List<QuizAssigning> classroomQuizzes = quizAssigningRepository.findByClassroomId(classId);

                    if (classroomQuizzes != null) {
                        for (QuizAssigning assigning : classroomQuizzes) {
                            if (assigning == null || assigning.getQuiz() == null)
                                continue;

                            if (quizMap.containsKey(assigning.getId()))
                                continue;

                            QuizTaking taking = quizTakingRepository
                                    .findByLearnerIdAndQuizAssigningId(student.getId(), assigning.getId())
                                    .orElse(null);

                            int finishedCount = 0;
                            boolean anyAttemptExists = false;
                            boolean hasUnfinished = false;
                            if (taking != null) {
                                List<Attempt> attempts = attemptRepository.findByQuizTakingId(taking.getId());
                                if (attempts != null) {
                                    finishedCount = (int) attempts.stream().filter(a -> a.getEndedAt() != null).count();
                                    anyAttemptExists = !attempts.isEmpty();
                                    hasUnfinished = attempts.stream().anyMatch(a -> a.getEndedAt() == null);
                                }
                            }

                            QuizDashboardInfo info = new QuizDashboardInfo();
                            info.setAssigning(assigning);
                            info.setAttemptsMade(finishedCount);
                            info.setAttemptsLeft(
                                    assigning.getMaxAttempt() == null || assigning.getMaxAttempt() == 0 ? -1
                                            : Math.max(0, assigning.getMaxAttempt() - finishedCount));
                            info.setHasStarted(anyAttemptExists);
                            info.setHasUnfinished(hasUnfinished);

                            quizMap.put(assigning.getId(), info);
                        }
                    }
                }
            }

            List<QuizDashboardInfo> allQuizzes = new ArrayList<>(quizMap.values());

            // Sắp xếp: Ưu tiên hạn gần nhất lên đầu
            allQuizzes.sort((a, b) -> {
                LocalDateTime d1 = a.getAssigning().getDueDate();
                LocalDateTime d2 = b.getAssigning().getDueDate();
                if (d1 == null && d2 == null)
                    return 0;
                if (d1 == null)
                    return 1;
                if (d2 == null)
                    return -1;
                return d1.compareTo(d2);
            });

            // Filter for the classroom dropdown
            List<ClassJoining> approvedClasses = joinedClasses.stream()
                    .filter(j -> j.getStatus() == JoinStatus.APPROVED)
                    .collect(Collectors.toList());

            model.addAttribute("allQuizzes", allQuizzes);
            model.addAttribute("now", now);
            model.addAttribute("currentUser", student);
            model.addAttribute("joinedClasses", approvedClasses);
        }

        return "student/student-quizzes";
    }

    private String getGreeting() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 5 && hour < 11)
            return "Chào buổi sáng";
        if (hour >= 11 && hour < 13)
            return "Chào buổi trưa";
        if (hour >= 13 && hour < 18)
            return "Chào buổi chiều";
        if (hour >= 18 && hour < 24)
            return "Chào buổi tối";
        return "Chào buổi đêm";
    }

    // Helper class for dashboard
    @lombok.Data
    public static class QuizDashboardInfo {
        private QuizAssigning assigning;
        private int attemptsMade;
        private int attemptsLeft; // -1 for unlimited
        private boolean hasStarted;
        private boolean hasUnfinished;
    }

    @GetMapping("/practice-history")
    public String practiceHistory(Model model) {
        return "student/student-practice-history";
    }

    @GetMapping("/quiz/play/{assigningId}")
    public String playQuiz(@PathVariable Long assigningId, Model model) {
        model.addAttribute("quizId", assigningId);
        return "student/quiz-play";
    }

    @GetMapping("/quiz/result/{attemptId}")
    public String quizResult(@PathVariable Long attemptId, Model model) {
        model.addAttribute("attemptId", attemptId);
        return "student/quiz-result";
    }

    @GetMapping("/quiz/history/{assigningId}")
    public String quizHistory(@PathVariable Long assigningId, Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByEmail(email).orElseThrow();

        QuizAssigning assigning = quizAssigningRepository.findById(assigningId).orElseThrow();
        QuizTaking taking = quizTakingRepository.findByLearnerIdAndQuizAssigningId(student.getId(), assigningId)
                .orElse(null);

        List<Attempt> attempts = new ArrayList<>();
        if (taking != null) {
            attempts = attemptRepository.findByQuizTakingIdOrderByStartedAtDesc(taking.getId());
        }

        model.addAttribute("assigning", assigning);
        model.addAttribute("attempts", attempts);
        return "student/student-quiz-history";
    }

    @GetMapping("/history")
    public String getHistory(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = userRepository.findByEmail(email).orElseThrow();

        // Get all finished quiz attempts
        List<Attempt> quizAttempts = attemptRepository
                .findByQuizTakingLearnerIdAndEndedAtIsNotNullOrderByStartedAtDesc(student.getId());

        // Get all finished practice sessions
        List<Practice> practiceHistory = practiceRepository
                .findByUserIdAndIsCompletedTrueOrderByCreatedAtDesc(student.getId());

        model.addAttribute("quizAttempts", quizAttempts);
        model.addAttribute("practiceHistory", practiceHistory);
        return "student/student-history";
    }
}
