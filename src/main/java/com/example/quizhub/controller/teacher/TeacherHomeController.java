package com.example.quizhub.controller.teacher;


import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.quizhub.dto.teacher.response.TeacherDashboardAssignmentDTO;
import com.example.quizhub.entity.User;
import com.example.quizhub.repository.AttemptViolationRepository;
import com.example.quizhub.repository.ClassroomRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.QuizAssigningRepository;
import com.example.quizhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import com.example.quizhub.repository.ClassJoiningRepository;

@Controller
@RequestMapping("/teacher")
@RequiredArgsConstructor
public class TeacherHomeController {

    private final UserRepository userRepository;
    private final ClassroomRepository classroomRepository;
    private final QuestionRepository questionRepository;
    private final QuizAssigningRepository quizAssigningRepository;
    private final AttemptViolationRepository attemptViolationRepository;
    private final ClassJoiningRepository classJoiningRepository;

    @GetMapping
    public String getTeacherDashboard(Principal principal, Model model) {
        User teacher = userRepository.findByEmail(principal.getName()).orElseThrow();
        
        // Stats
        model.addAttribute("classroomCount", classroomRepository.countByCreatorId(teacher.getId()));
        model.addAttribute("questionCount", questionRepository.countByCreatorId(teacher.getId()));
        model.addAttribute("studentCount", classJoiningRepository.countDistinctLearnersByTeacherId(teacher.getId()));
        
        List<TeacherDashboardAssignmentDTO> assignments = quizAssigningRepository.findByClassroomCreatorId(teacher.getId()).stream()
            .map(qa -> {
                String status = "UPCOMING";
                LocalDateTime now = LocalDateTime.now();
                if (now.isAfter(qa.getStartDate()) && now.isBefore(qa.getDueDate())) status = "LIVE";
                else if (now.isAfter(qa.getDueDate())) status = "ENDED";

                return TeacherDashboardAssignmentDTO.builder()
                        .assigningId(qa.getId())
                        .classroomName(qa.getClassroom().getName())
                        .quizTitle(qa.getQuiz().getTitle())
                        .violationCount(attemptViolationRepository.countByAssigningId(qa.getId()))
                        .status(status)
                        .build();
            })
            .collect(Collectors.toList());

        model.addAttribute("assignments", assignments);
        model.addAttribute("liveQuizCount", assignments.stream().filter(as -> "LIVE".equals(as.getStatus())).count());
        model.addAttribute("currentUser", teacher);
        
        return "teacher/teacher-home";
    }
}
