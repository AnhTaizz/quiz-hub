package com.example.quizhub.controller.student;

import com.example.quizhub.entity.ClassJoining;
import com.example.quizhub.entity.ClassTopic;
import com.example.quizhub.entity.QuizAssigning;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.JoinStatus;
import com.example.quizhub.repository.ClassJoiningRepository;
import com.example.quizhub.repository.ClassTopicRepository;
import com.example.quizhub.repository.ClassroomRepository;
import com.example.quizhub.repository.QuizAssigningRepository;
import com.example.quizhub.service.classroom.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student/classrooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentClassroomWebController {

    private final ClassJoiningRepository classJoiningRepository;
    private final ClassroomService classroomService;
    private final ClassroomRepository classroomRepository;
    private final QuizAssigningRepository quizAssigningRepository;
    private final ClassTopicRepository classTopicRepository;

    @GetMapping
    public String listClassrooms(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            List<JoinStatus> allowedStatuses = List.of(
                    JoinStatus.PENDING,
                    JoinStatus.APPROVED);
            List<ClassJoining> joinedClasses = classJoiningRepository.findByLearnerIdAndStatusIn(user.getId(),
                    allowedStatuses).stream().filter(j -> j.getClassroom() != null).collect(Collectors.toList());
            model.addAttribute("joinedClasses", joinedClasses);
            model.addAttribute("currentUser", user);
        }
        return "student/student-classrooms";
    }

    @PostMapping("/join")
    public String joinClass(@RequestParam String code, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try {
                classroomService.joinClass(user.getEmail(), code);
                redirectAttributes.addFlashAttribute("successMessage", "Yêu cầu tham gia đã được gửi thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            }
        }
        return "redirect:/student/classrooms";
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String classroomDetailPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            // Check if the student is approved to join the class
            ClassJoining joining = classJoiningRepository.findByClassroomIdAndLearnerId(id, user.getId())
                    .orElse(null);

            if (joining == null || joining.getStatus() != JoinStatus.APPROVED) {
                return "redirect:/student/classrooms";
            }

            com.example.quizhub.entity.Classroom classroom = classroomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Classroom not found"));

            List<QuizAssigning> assignedQuizzes = quizAssigningRepository.findByClassroomId(id).stream()
                    .filter(a -> !Boolean.TRUE.equals(a.getIsHidden()))
                    .filter(a -> {
                        if (a.getAssignedStudentIds() == null || a.getAssignedStudentIds().isBlank()) {
                            return true;
                        }
                        List<String> allowedIds = java.util.Arrays.asList(a.getAssignedStudentIds().split(","));
                        return allowedIds.contains(String.valueOf(user.getId()));
                    })
                    .collect(java.util.stream.Collectors.toList());
            List<ClassTopic> topics = classTopicRepository.findByClassroomId(id);

            model.addAttribute("classroom", classroom);
            model.addAttribute("assignedQuizzes", assignedQuizzes);
            model.addAttribute("topics", topics);
            model.addAttribute("currentUser", user);
            model.addAttribute("now", java.time.LocalDateTime.now());
        }
        return "student/student-classroom-detail";
    }
}
