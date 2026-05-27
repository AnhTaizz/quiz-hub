package com.example.quizhub.controller.student;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.QuizAssigning;
import com.example.quizhub.entity.ClassJoining;
import com.example.quizhub.entity.Attempt;
import com.example.quizhub.dto.student.StudentHomeDashboardDTO;
import com.example.quizhub.dto.student.QuizDashboardInfoDTO;
import com.example.quizhub.service.PracticeService;
import com.example.quizhub.service.student.StudentHomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
@Slf4j
public class StudentHomeController {

    private final PracticeService practiceService;
    private final StudentHomeService studentHomeService;

    @GetMapping
    public String home(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = studentHomeService.getStudentByEmail(email);
        if (student != null) {
            StudentHomeDashboardDTO data = studentHomeService.getDashboardData(email);
            if (data != null) {
                model.addAttribute("totalCompleted", data.getTotalCompleted());
                model.addAttribute("quizAvg", data.getQuizAvg());
                model.addAttribute("practiceAvg", data.getPracticeAvg());
                model.addAttribute("assignedQuizzes", data.getAssignedQuizzes());
                model.addAttribute("pendingCount", data.getPendingCount());
                model.addAttribute("pendingThisWeekCount", data.getPendingThisWeekCount());
            }
            model.addAttribute("greeting", getGreeting());
        }
        return "student/student-home";
    }

    @GetMapping("/quizzes")
    public String listAllQuizzes(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User student = studentHomeService.getStudentByEmail(email);
        if (student != null) {
            List<QuizDashboardInfoDTO> allQuizzes = studentHomeService.getAllQuizzes(email);
            List<ClassJoining> approvedClasses = studentHomeService.getApprovedClasses(email);
            model.addAttribute("allQuizzes", allQuizzes);
            model.addAttribute("now", LocalDateTime.now());
            model.addAttribute("currentUser", student);
            model.addAttribute("joinedClasses", approvedClasses);
        }
        return "student/student-quizzes";
    }

    private String getGreeting() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 5 && hour < 11) return "Chao buoi sang";
        if (hour >= 11 && hour < 13) return "Chao buoi trua";
        if (hour >= 13 && hour < 18) return "Chao buoi chieu";
        if (hour >= 18 && hour < 24) return "Chao buoi toi";
        return "Chao buoi dem";
    }

    @GetMapping("/practice-history")
    public String practiceHistory(Model model) { return "student/student-practice-history"; }

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
        QuizAssigning assigning = studentHomeService.getQuizAssigningById(assigningId);
        List<Attempt> attempts = studentHomeService.getQuizHistory(assigningId, email);
        model.addAttribute("assigning", assigning);
        model.addAttribute("attempts", attempts);
        return "student/student-quiz-history";
    }

    @GetMapping("/history")
    public String getHistory(Model model) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        List<Attempt> quizAttempts = studentHomeService.getAllQuizAttempts(email);
        List<com.example.quizhub.dto.practice.PracticeHistoryResponseDTO> practiceHistory = practiceService.getMyPracticeHistory();
        model.addAttribute("quizAttempts", quizAttempts);
        model.addAttribute("practiceHistory", practiceHistory);
        return "student/student-history";
    }
}
