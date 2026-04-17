package com.example.quizhub.controller.student;

import com.example.quizhub.entity.ClassJoining;
import com.example.quizhub.entity.User;
import com.example.quizhub.repository.ClassJoiningRepository;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.classroom.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/student/classrooms")
@RequiredArgsConstructor
public class StudentClassroomWebController {

    private final ClassJoiningRepository classJoiningRepository;
    private final ClassroomService classroomService;
    private final UserRepository userRepository;

    @GetMapping
    public String listClassrooms(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            List<ClassJoining> joinedClasses = classJoiningRepository.findByLearnerId(user.getId());
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
}
