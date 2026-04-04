package com.example.quizhub.service.quiztaking;

import com.example.quizhub.dto.quiztaking.response.QuizTakingResponseDTO;

public interface QuizTakingService {
    QuizTakingResponseDTO startQuizAttempt(Long studentId, Long quizAssigningId);
}
