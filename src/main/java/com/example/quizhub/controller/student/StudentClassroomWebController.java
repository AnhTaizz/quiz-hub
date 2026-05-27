package com.example.quizhub.controller.student;

import com.example.quizhub.entity.*;
import com.example.quizhub.entity.enums.JoinStatus;
import com.example.quizhub.service.classroom.ClassroomService;
import com.example.quizhub.service.student.StudentClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/student/classrooms")
@PreAuthorize("hasRole('STUDENT')")
@RequiredArgsConstructor
public class StudentClassroomWebController {

    private final ClassroomService classroomService;
    private final StudentClassroomService studentClassroomService;

    @GetMapping
    public String listClassrooms(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            List<ClassJoining> joinedClasses = studentClassroomService.getJoinedClassrooms(user.getId());
            model.addAttribute("joinedClasses", joinedClasses);
            model.addAttribute("currentUser", user);
        }
        return "student/student-classrooms";
    }

    @PostMapping("/join")
    public String joinClass(@RequestParam String classCode, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try {
                classroomService.joinClass(user.getEmail(), classCode);
                redirectAttributes.addFlashAttribute("successMessage", "Yeu cau tham gia lop hoc da duoc gui!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Loi: " + e.getMessage());
            }
        }
        return "redirect:/student/classrooms";
    }

    @GetMapping("/{id}")
    public String classroomDetailPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            ClassJoining joining = studentClassroomService.getJoiningStatus(id, user.getId());
            if (joining == null || joining.getStatus() != JoinStatus.APPROVED) {
                return "redirect:/student/classrooms";
            }

            Classroom classroom = studentClassroomService.getClassroomById(id);
            List<QuizAssigning> assignedQuizzes = studentClassroomService.getAssignedQuizzesForClassroom(id, user.getId());
            List<ClassTopic> topics = studentClassroomService.getClassTopics(id);

            model.addAttribute("classroom", classroom);
            model.addAttribute("assignedQuizzes", assignedQuizzes);
            model.addAttribute("topics", topics);
            model.addAttribute("currentUser", user);
            model.addAttribute("now", LocalDateTime.now());
        }
        return "student/student-classroom-detail";
    }
}
