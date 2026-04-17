package com.example.quizhub.controller.teacher;

import com.example.quizhub.dto.classroom.request.ClassroomRequestDTO;
import com.example.quizhub.entity.ClassJoining;
import com.example.quizhub.entity.Classroom;
import com.example.quizhub.entity.JoinStatus;
import com.example.quizhub.entity.User;
import com.example.quizhub.repository.ClassJoiningRepository;
import com.example.quizhub.repository.ClassroomRepository;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.classroom.ClassroomService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/teacher/classrooms")
@RequiredArgsConstructor
public class TeacherClassroomWebController {

    private final ClassroomRepository classroomRepository;
    private final UserRepository userRepository;
    private final ClassroomService classroomService;
    private final ClassJoiningRepository classJoiningRepository;

    @GetMapping
    public String classroomPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            List<Classroom> classrooms = classroomRepository.findByCreatorId(user.getId());
            model.addAttribute("classrooms", classrooms);
            model.addAttribute("currentUser", user);
        }
        return "teacher/teacher-classrooms";
    }

    @PostMapping
    public String createClassroom(@ModelAttribute ClassroomRequestDTO request, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try {
                classroomService.createClassroom(user.getEmail(), request);
                redirectAttributes.addFlashAttribute("successMessage", "Tạo lớp học thành công!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            }
        }
        return "redirect:/teacher/classrooms";
    }

    @GetMapping("/{id}/members")
    public String memberManagementPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            Classroom classroom = classroomRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Classroom not found"));
            
            // Security check
            if (!classroom.getCreator().getId().equals(user.getId())) {
                return "redirect:/teacher/classrooms";
            }

            List<ClassJoining> activeMembers = classJoiningRepository.findByClassroomIdAndStatus(id, JoinStatus.APPROVED);
            List<ClassJoining> pendingRequests = classJoiningRepository.findByClassroomIdAndStatus(id, JoinStatus.PENDING);

            model.addAttribute("classroom", classroom);
            model.addAttribute("activeMembers", activeMembers);
            model.addAttribute("pendingRequests", pendingRequests);
            model.addAttribute("currentUser", user);
        }
        return "teacher/teacher-classroom-members";
    }

    @PostMapping("/{id}/approve/{joiningId}")
    public String approveMember(@PathVariable Long id, @PathVariable Long joiningId, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try {
                classroomService.approveJoinRequest(joiningId, user.getEmail());
                redirectAttributes.addFlashAttribute("successMessage", "Đã phê duyệt thành viên!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            }
        }
        return "redirect:/teacher/classrooms/" + id + "/members";
    }

    @PostMapping("/{id}/reject/{joiningId}")
    public String rejectMember(@PathVariable Long id, @PathVariable Long joiningId, RedirectAttributes redirectAttributes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try {
                classroomService.rejectJoinRequest(joiningId, user.getEmail());
                redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối yêu cầu tham gia!");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            }
        }
        return "redirect:/teacher/classrooms/" + id + "/members";
    }
}
