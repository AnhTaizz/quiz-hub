package com.example.quizhub.controller.student.rest;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.quizhub.dto.quiztaking.request.QuizSubmitRequestDTO;
import com.example.quizhub.dto.quiztaking.response.QuizTakingResponseDTO;
import com.example.quizhub.entity.Attempt;
import com.example.quizhub.entity.User;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.quiztaking.QuizTakingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/student/quiz")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentQuizRestController {

    private final QuizTakingService quizTakingService;
    private final UserRepository userRepository;
    private final com.example.quizhub.service.quiz.QuizService quizService;

    @GetMapping("/mine")
    public ResponseEntity<java.util.List<com.example.quizhub.dto.quiz.QuizSummaryDTO>> getMyQuizzes() {
        return ResponseEntity.ok(quizService.getMyQuizzes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.example.quizhub.dto.quiz.QuizResponseDTO> getQuiz(@PathVariable String id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    @PostMapping
    public ResponseEntity<com.example.quizhub.dto.quiz.QuizResponseDTO> createQuiz(@RequestBody @jakarta.validation.Valid com.example.quizhub.dto.quiz.QuizRequestDTO request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(quizService.createNewQuiz(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<com.example.quizhub.dto.quiz.QuizResponseDTO> updateQuiz(
            @PathVariable String id,
            @RequestBody @jakarta.validation.Valid com.example.quizhub.dto.quiz.QuizRequestDTO request) {
        return ResponseEntity.ok(quizService.updateQuiz(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuiz(@PathVariable String id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/start")
    public ResponseEntity<QuizTakingResponseDTO> startQuiz(Principal principal, @RequestParam Long assigningId) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(quizTakingService.startQuizAttempt(user.getId(), assigningId));
    }

    @PostMapping("/submit")
    public ResponseEntity<java.util.Map<String, Object>> submitQuiz(Principal principal, @RequestBody QuizSubmitRequestDTO request) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        Attempt attempt = quizTakingService.submitQuizAttempt(user.getId(), request);
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("id", attempt.getId());
        response.put("score", attempt.getResult());
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-answer")
    public ResponseEntity<Void> saveAnswer(Principal principal, 
                                         @RequestParam Long attemptId, 
                                         @RequestParam Long questionId, 
                                         @RequestBody java.util.List<Long> answerIds) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        quizTakingService.saveAnswer(user.getId(), attemptId, questionId, answerIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/result")
    public ResponseEntity<com.example.quizhub.dto.quiztaking.response.QuizResultResponseDTO> getQuizResult(Principal principal, @RequestParam Long attemptId) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(quizTakingService.getQuizResult(user.getId(), attemptId));
    }

    @GetMapping("/start-personal")
    public ResponseEntity<QuizTakingResponseDTO> startPersonalQuiz(Principal principal, @RequestParam String quizId) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(quizTakingService.startPersonalQuizAttempt(user.getId(), quizId));
    }

    @GetMapping("/history-quiz")
    public ResponseEntity<java.util.List<com.example.quizhub.dto.quiztaking.response.QuizAttemptSummaryDTO>> getQuizHistory(Principal principal, @RequestParam String quizId) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(quizTakingService.getQuizAttempts(user.getId(), quizId));
    }

    @GetMapping("/resume")
    public ResponseEntity<QuizTakingResponseDTO> getQuizState(Principal principal, @RequestParam Long attemptId) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(quizTakingService.getQuizTakingState(user.getId(), attemptId));
    }
}
