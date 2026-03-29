package com.example.quizhub.service.quiz;

import com.example.quizhub.dto.quiz.request.QuizRequestDTO;
import com.example.quizhub.dto.quiz.response.QuizResponseDTO;

public interface QuizService {
    QuizResponseDTO createNewQuiz(QuizRequestDTO request);

    QuizResponseDTO getQuizById(String id);

    QuizResponseDTO updateQuiz(String id, QuizRequestDTO request);

    void deleteQuiz(String id);
}
