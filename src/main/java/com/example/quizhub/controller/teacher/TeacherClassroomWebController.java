package com.example.quizhub.controller.teacher;

import com.example.quizhub.dto.classroom.request.ClassroomRequestDTO;
import com.example.quizhub.entity.*;
import com.example.quizhub.service.CategoryService;
import com.example.quizhub.service.classroom.ClassroomService;
import com.example.quizhub.service.teacher.TeacherClassroomWebService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/teacher/classrooms")
@RequiredArgsConstructor
public class TeacherClassroomWebController {
    private final ClassroomService classroomService;
    private final TeacherClassroomWebService teacherClassroomWebService;
    private final CategoryService categoryService;

    @GetMapping
    public String classroomPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            List<Classroom> classrooms = teacherClassroomWebService.getTeacherClassrooms(user.getId());
            Map<Long, Long> memberCounts = teacherClassroomWebService.getActiveMemberCounts(classrooms);
            model.addAttribute("classrooms", classrooms);
            model.addAttribute("memberCounts", memberCounts);
            model.addAttribute("currentUser", user);
        }
        return "teacher/teacher-classrooms";
    }

    @PostMapping
    public String createClassroom(@ModelAttribute ClassroomRequestDTO request, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try { classroomService.createClassroom(user.getEmail(), request); ra.addFlashAttribute("successMessage", "Tao lop hoc thanh cong!"); }
            catch (Exception e) { ra.addFlashAttribute("errorMessage", "Loi: " + e.getMessage()); }
        }
        return "redirect:/teacher/classrooms";
    }

    @GetMapping("/{id}/members")
    public String memberManagementPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            Classroom classroom = teacherClassroomWebService.getClassroomByIdAndTeacher(id, user.getId());
            if (classroom == null) return "redirect:/teacher/classrooms";
            model.addAttribute("classroom", classroom);
            model.addAttribute("activeMembers", teacherClassroomWebService.getActiveMembers(id));
            model.addAttribute("pendingRequests", teacherClassroomWebService.getPendingMembers(id));
            model.addAttribute("currentUser", user);
        }
        return "teacher/teacher-classroom-members";
    }

    @PostMapping("/{id}/approve/{joiningId}")
    public String approveMember(@PathVariable Long id, @PathVariable Long joiningId, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try { classroomService.approveJoinRequest(joiningId, user.getEmail()); ra.addFlashAttribute("successMessage", "Da phe duyet thanh vien!"); }
            catch (Exception e) { ra.addFlashAttribute("errorMessage", "Loi: " + e.getMessage()); }
        }
        return "redirect:/teacher/classrooms/" + id + "/members";
    }

    @PostMapping("/{id}/reject/{joiningId}")
    public String rejectMember(@PathVariable Long id, @PathVariable Long joiningId, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try { classroomService.rejectJoinRequest(joiningId, user.getEmail()); ra.addFlashAttribute("successMessage", "Da tu choi yeu cau!"); }
            catch (Exception e) { ra.addFlashAttribute("errorMessage", "Loi: " + e.getMessage()); }
        }
        return "redirect:/teacher/classrooms/" + id + "/members";
    }

    @PostMapping("/{id}/remove/{studentId}")
    public String removeStudent(@PathVariable Long id, @PathVariable Long studentId, RedirectAttributes ra) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            try { classroomService.removeStudentFromClass(id, studentId, user.getEmail()); ra.addFlashAttribute("successMessage", "Da xoa sinh vien!"); }
            catch (Exception e) { ra.addFlashAttribute("errorMessage", "Loi: " + e.getMessage()); }
        }
        return "redirect:/teacher/classrooms/" + id + "/members";
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional
    public String classroomDetailPage(@PathVariable Long id, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            Classroom classroom = teacherClassroomWebService.getClassroomByIdAndTeacher(id, user.getId());
            if (classroom == null) return "redirect:/teacher/classrooms";
            model.addAttribute("classroom", classroom);
            model.addAttribute("assignedQuizzes", teacherClassroomWebService.getAssignedQuizzes(id));
            model.addAttribute("quizzes", teacherClassroomWebService.getTeacherQuizzes(user.getId()));
            model.addAttribute("topics", teacherClassroomWebService.getClassTopics(id));
            model.addAttribute("categories", teacherClassroomWebService.getTeacherCategories(user.getId()));
            model.addAttribute("myCategories", categoryService.getMyCategories());
            model.addAttribute("publicCategories", categoryService.getPublicCategories());
            model.addAttribute("today", LocalDateTime.now().withSecond(0).withNano(0));
            model.addAttribute("nextWeek", LocalDateTime.now().plusDays(7).withHour(23).withMinute(59).withSecond(0).withNano(0));
            model.addAttribute("currentUser", user);
        }
        return "teacher/teacher-classroom-detail";
    }
}
