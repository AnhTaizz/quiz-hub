package com.example.quizhub.controller.teacher;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.quizhub.dto.quizassigning.request.QuizAssigningRequestDTO;
import com.example.quizhub.dto.quizassigning.response.QuizAssigningResponseDTO;
import com.example.quizhub.service.quizassigning.QuizAssigningService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teacher/quiz-assigning")
@RequiredArgsConstructor
public class TeacherQuestionAssigningController {
    private final QuizAssigningService quizAssigningService;

    @PostMapping
    public ResponseEntity<QuizAssigningResponseDTO> create(@RequestBody QuizAssigningRequestDTO requestDTO){
        QuizAssigningResponseDTO responseDTO = quizAssigningService.create(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);
    }

}
