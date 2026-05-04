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
import com.example.quizhub.entity.JoinStatus;
import com.example.quizhub.entity.Attempt;
import com.example.quizhub.entity.QuizTaking;
import com.example.quizhub.entity.enums.TakingStatus;
import com.example.quizhub.repository.AttemptRepository;
import com.example.quizhub.repository.PracticeRepository;
import com.example.quizhub.repository.QuizTakingRepository;
import com.example.quizhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
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
            long attemptCount = attemptRepository.countByQuizTakingLearnerId(student.getId());
            long practiceCount = practiceRepository.countByUserId(student.getId());
            long totalCompleted = attemptCount + practiceCount;

            List<Attempt> attempts = attemptRepository.findByQuizTakingLearnerId(student.getId());
            List<com.example.quizhub.entity.Practice> practices = practiceRepository.findByUserId(student.getId());

            BigDecimal totalScoreSum = BigDecimal.ZERO;
            BigDecimal avgScore = BigDecimal.ZERO;
            int totalCount = attempts.size() + practices.size();

            if (totalCount > 0) {
                // Sum scores from attempts
                BigDecimal attemptsSum = attempts.stream()
                        .map(a -> a.getResult() != null ? a.getResult() : BigDecimal.ZERO)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Sum scores from practices (calculate on the fly: correct/total * 10)
                BigDecimal practicesSum = practices.stream()
                        .map(p -> {
                            if (p.getTotalQuestions() != null && p.getTotalQuestions() > 0) {
                                double calc = (p.getCorrectAnswers() * 10.0) / p.getTotalQuestions();
                                return BigDecimal.valueOf(calc);
                            }
                            return BigDecimal.ZERO;
                        })
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                totalScoreSum = attemptsSum.add(practicesSum);
                avgScore = totalScoreSum.divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP);
            }

            // Get all assigned quizzes from joined classrooms
            List<ClassJoining> joinedClasses = classJoiningRepository.findByLearnerId(student.getId());
            List<QuizAssigning> assignedQuizzes = new ArrayList<>();
            for (ClassJoining joining : joinedClasses) {
                if (joining.getStatus() == JoinStatus.APPROVED) {
                    assignedQuizzes.addAll(quizAssigningRepository.findByClassroomId(joining.getClassroom().getId()));
                }
            }

            // Filter out completed ones and map to a consistent structure
            // For now, let's just send the AssignedQuizzes and handle status
            model.addAttribute("totalCompleted", totalCompleted);
            model.addAttribute("avgScore", avgScore);
            model.addAttribute("assignedQuizzes", assignedQuizzes);
            model.addAttribute("pendingCount", assignedQuizzes.size());
        }

        return "student/student-home";
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
}
