package com.example.quizhub.service.quiztaking;

import com.example.quizhub.dto.quiztaking.request.QuizSubmitRequestDTO;
import com.example.quizhub.dto.quiztaking.response.QuizTakingResponseDTO;
import com.example.quizhub.entity.Attempt;

public interface QuizTakingService {
    QuizTakingResponseDTO startQuizAttempt(Long studentId, Long quizAssigningId);

    QuizTakingResponseDTO startPersonalQuizAttempt(Long studentId, String quizId);

    Attempt submitQuizAttempt(Long studentId, QuizSubmitRequestDTO requestDTO);

    void saveAnswer(Long studentId, Long attemptId, Long questionId, java.util.List<Long> answerIds);

    com.example.quizhub.dto.quiztaking.response.QuizResultResponseDTO getQuizResult(Long studentId, Long attemptId);

    java.util.List<com.example.quizhub.dto.quiztaking.response.QuizAttemptSummaryDTO> getQuizAttempts(Long studentId, String quizId);
    
    QuizTakingResponseDTO getQuizTakingState(Long studentId, Long attemptId);
}
