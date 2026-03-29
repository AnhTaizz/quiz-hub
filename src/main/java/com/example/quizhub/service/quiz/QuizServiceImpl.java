package com.example.quizhub.service.quiz;

import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.quiz.request.QuizRequestDTO;
import com.example.quizhub.dto.quiz.response.QuizResponseDTO;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Quiz;
import com.example.quizhub.entity.User;
import com.example.quizhub.mapper.QuizMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuizRepository;
import com.example.quizhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final QuizMapper quizMapper;

    // Helper

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng: " + email));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + categoryId));
    }

    private Quiz findQuiz(String id) {
        return quizRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề thi với id: " + id));
    }

    // API

    @Override
    @Transactional
    public QuizResponseDTO createNewQuiz(QuizRequestDTO request) {
        Quiz quiz = quizMapper.toQuiz(request);
        quiz.setCreator(getCurrentUser());
        quiz.setCategory(resolveCategory(request.getCategoryId()));
        quiz.setIsEnable(true);

        return quizMapper.toQuizResponse(quizRepository.save(quiz));
    }

    @Override
    public QuizResponseDTO getQuizById(String id) {
        return quizMapper.toQuizResponse(findQuiz(id));
    }

    @Override
    @Transactional
    public QuizResponseDTO updateQuiz(String id, QuizRequestDTO request) {
        Quiz quiz = findQuiz(id);

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setImageUrl(request.getImageUrl());
        quiz.setIsDraft(request.getIsDraft());
        quiz.setIsExam(request.getIsExam());
        quiz.setCategory(resolveCategory(request.getCategoryId()));

        return quizMapper.toQuizResponse(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public void deleteQuiz(String id) {
        Quiz quiz = findQuiz(id);
        // Soft delete
        quiz.setIsEnable(false);
        quizRepository.save(quiz);
    }
}
