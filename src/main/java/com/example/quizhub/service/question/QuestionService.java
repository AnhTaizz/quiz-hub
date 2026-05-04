package com.example.quizhub.service.question;

import org.springframework.data.domain.Page;

import com.example.quizhub.dto.question.QuestionRequestDTO;
import com.example.quizhub.dto.question.QuestionResponseDTO;
import com.example.quizhub.entity.enums.QuestionType;

public interface QuestionService {
    QuestionResponseDTO createNewQuestion(Long userId, QuestionRequestDTO request);

    QuestionResponseDTO updateQuestion(Long userId, Long id, QuestionRequestDTO request);

    QuestionResponseDTO getQuestionById(Long id);

    void deleteQuestion(Long userId, Long id);

    Page<QuestionResponseDTO> searchMyQuestion(Long userId, Long categoryId,
                                                    QuestionType type,
                                                    String keyword,
                                                    int page,
                                                    int size,
                                                    String sortBy,
                                                    String sortDir);

    Page<QuestionResponseDTO> searchPublicQuestion(Long categoryId,
                                                   QuestionType type,
                                                   String keyword,
                                                   int page,
                                                   int size,
                                                   String sortBy,
                                                   String sortDir);

    //Teacher share question
    void requestShareQuestion(Long questionId, Long teacherId);

    //Admin
    void approveQuestion(Long questionId, Long categoryId);

    void rejectQuestion(Long questionId);

    void deleteQuestionByAdmin(Long questionId);

    void moveQuestionByAdmin(Long questionId, Long categoryId);
}
