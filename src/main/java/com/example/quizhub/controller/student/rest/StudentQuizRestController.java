package com.example.quizhub.controller.student.rest;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
}
