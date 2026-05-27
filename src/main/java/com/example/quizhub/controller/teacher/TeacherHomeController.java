package com.example.quizhub.controller.teacher;

import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.quizhub.dto.teacher.response.TeacherHomeDashboardDTO;
import com.example.quizhub.service.teacher.TeacherHomeService;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherHomeController {
    private final TeacherHomeService teacherHomeService;

    @GetMapping
    public String getTeacherDashboard(Principal principal, Model model) {
        TeacherHomeDashboardDTO data = teacherHomeService.getDashboardData(principal.getName());
        model.addAttribute("classroomCount", data.getClassroomCount());
        model.addAttribute("questionCount", data.getQuestionCount());
        model.addAttribute("studentCount", data.getStudentCount());
        model.addAttribute("assignments", data.getAssignments());
        model.addAttribute("liveQuizCount", data.getLiveQuizCount());
        model.addAttribute("currentUser", data.getCurrentUser());
        return "teacher/teacher-home";
    }
}
