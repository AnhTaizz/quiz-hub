package com.example.quizhub.controller.teacher;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher/quizzes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherQuizWebController {

    @GetMapping
    public String getQuizzesPage() {
        return "teacher/teacher-quizzes";
    }

    @GetMapping("/create")
    public String getCreateQuizPage(@RequestParam(required = false) Long categoryId, Model model) {
        model.addAttribute("categoryId", categoryId);
        return "teacher/teacher-quiz-create";
    }

    @GetMapping("/{id}/edit")
    public String getEditQuizPage(@PathVariable String id, Model model) {
        model.addAttribute("quizId", id);
        return "teacher/teacher-quiz-create";
    }

    @GetMapping("/{id}/preview")
    public String getPreviewQuizPage(@PathVariable String id, Model model) {
        model.addAttribute("quizId", id);
        return "teacher/teacher-quiz-preview";
    }
}
