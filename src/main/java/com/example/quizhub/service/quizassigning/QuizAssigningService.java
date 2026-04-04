package com.example.quizhub.service.quizassigning;

import com.example.quizhub.dto.quizassigning.request.QuizAssigningRequestDTO;
import com.example.quizhub.dto.quizassigning.response.QuizAssigningResponseDTO;

public interface QuizAssigningService {
    QuizAssigningResponseDTO create(QuizAssigningRequestDTO requestDTO);
}
