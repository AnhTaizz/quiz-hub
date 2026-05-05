package com.example.quizhub.service.quiz;

import java.util.List;

import com.example.quizhub.dto.quiz.QuizRequestDTO;
import com.example.quizhub.dto.quiz.QuizResponseDTO;
import com.example.quizhub.dto.quiz.QuizSummaryDTO;

public interface QuizService {
    QuizResponseDTO createNewQuiz(QuizRequestDTO request);

    QuizResponseDTO getQuizById(String id);

    QuizResponseDTO updateQuiz(String id, QuizRequestDTO request);

    void deleteQuiz(String id);

    /** Quiz công khai (published) trong 1 danh mục */
    List<QuizSummaryDTO> getPublicQuizzesByCategoryId(Long categoryId);

    /** Quiz cá nhân của user hiện tại trong 1 danh mục */
    List<QuizSummaryDTO> getMyQuizzesByCategoryId(Long categoryId);

    /** Toàn bộ Quiz cá nhân của user hiện tại */
    List<QuizSummaryDTO> getMyQuizzes();

    /** Generate quiz from a category's public questions */
    QuizResponseDTO generateQuizFromCategory(com.example.quizhub.dto.quiz.QuizGenerateRequestDTO request);
}
