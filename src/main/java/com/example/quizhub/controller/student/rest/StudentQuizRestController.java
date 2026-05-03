package com.example.quizhub.controller.student.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.quizhub.dto.quiz.QuizRequestDTO;
import com.example.quizhub.dto.quiz.QuizResponseDTO;
import com.example.quizhub.dto.quiz.QuizGenerateRequestDTO;
import com.example.quizhub.service.quiz.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/quizzes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentQuizRestController {

    private final QuizService quizService;

    @PostMapping
    public ResponseEntity<QuizResponseDTO> createQuiz(@RequestBody @Valid QuizRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.createNewQuiz(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizResponseDTO> getQuiz(@PathVariable String id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizResponseDTO> updateQuiz(
            @PathVariable String id,
            @RequestBody @Valid QuizRequestDTO request) {
        return ResponseEntity.ok(quizService.updateQuiz(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable String id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/generate-from-category")
    public ResponseEntity<QuizResponseDTO> generateQuizFromCategory(
            @RequestBody @Valid QuizGenerateRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(quizService.generateQuizFromCategory(request));
    }
}
