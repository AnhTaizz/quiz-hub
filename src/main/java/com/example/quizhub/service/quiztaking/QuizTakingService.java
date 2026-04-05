package com.example.quizhub.service.quiztaking;

import com.example.quizhub.dto.quiztaking.request.QuizSubmitRequestDTO;
import com.example.quizhub.dto.quiztaking.response.QuizTakingResponseDTO;
import com.example.quizhub.entity.Attempt;

public interface QuizTakingService {
    QuizTakingResponseDTO startQuizAttempt(Long studentId, Long quizAssigningId);

    Attempt submitQuizAttempt(Long studentId, QuizSubmitRequestDTO requestDTO);
}
