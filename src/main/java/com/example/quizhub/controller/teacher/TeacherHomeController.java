package com.example.quizhub.controller.teacher;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
public class TeacherHomeController {

    @GetMapping
    public String getTeacherDashboard(Model model) {
        return "teacher/teacher-home";
    }
}
