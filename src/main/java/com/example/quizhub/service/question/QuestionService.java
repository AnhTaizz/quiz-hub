package com.example.quizhub.service.question;

import org.springframework.data.domain.Page;

import com.example.quizhub.dto.question.request.QuestionRequestDTO;
import com.example.quizhub.dto.question.response.QuestionResponseDTO;
import com.example.quizhub.entity.enums.QuestionType;

public interface QuestionService {
    QuestionResponseDTO createNewQuestion(Long userId, QuestionRequestDTO request);

    QuestionResponseDTO updateQuestion(Long id, QuestionRequestDTO request);

    void deleteQuestion(Long id);

    Page<QuestionResponseDTO> getAllQuestions(Long categoryId,
                                                QuestionType type,
                                                String keyword,
                                                int page,
                                                int size,
                                                String sortBy,
                                                String sortDir);
}
