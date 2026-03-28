package com.example.quizhub.service;

import java.util.List;

import com.example.quizhub.dto.request.QuestionCreationRequestDTO;
import com.example.quizhub.dto.response.QuestionResponseDTO;

public interface QuestionService {
    QuestionResponseDTO createNewQuestion(Long userId, QuestionCreationRequestDTO request);

    QuestionResponseDTO updateQuestion(Long id, QuestionCreationRequestDTO request);

    QuestionResponseDTO getQuestionById(Long id);

    void deleteQuestion(Long id);

    List<QuestionResponseDTO> getAllQuestions();
}
