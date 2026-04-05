package com.example.quizhub.service.quiz;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.quiz.request.QuizRequestDTO;
import com.example.quizhub.dto.quiz.response.QuizResponseDTO;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.Quiz;
import com.example.quizhub.entity.User;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.mapper.QuizMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuestionRepository;
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
    private final QuestionRepository questionRepository;

    // Helper

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Quiz findQuiz(String id) {
        return quizRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
    }

    // API

    @Override
    @Transactional
    public QuizResponseDTO createNewQuiz(QuizRequestDTO request) {
        Quiz quiz = quizMapper.toEntity(request);
        quiz.setCreator(getCurrentUser());
        quiz.setCategory(resolveCategory(request.getCategoryId()));
        quiz.setIsEnable(true);

        List<Question> questions = questionRepository.findAllById(request.getQuestionIds());

        if (questions.size() != request.getQuestionIds().size()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }

        quiz.setQuestions(questions);
        // Khi gọi save(), Hibernate sẽ tự động chèn data vào bảng trung gian _question_creating
        return quizMapper.toResponseDTO(quizRepository.save(quiz));
    }

    @Override
    public QuizResponseDTO getQuizById(String id) {
        return quizMapper.toResponseDTO(findQuiz(id));
    }

    @Override
    @Transactional
    public QuizResponseDTO updateQuiz(String id, QuizRequestDTO request) {
        Quiz quiz = findQuiz(id);

        if(!quiz.getCreator().getId().equals(getCurrentUser().getId())){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setImageUrl(request.getImageUrl());
        quiz.setIsDraft(request.getIsDraft());
        quiz.setIsExam(request.getIsExam());
        quiz.setCategory(resolveCategory(request.getCategoryId()));

        List<Question> questions = questionRepository.findAllById(request.getQuestionIds());
        if (questions.size() != request.getQuestionIds().size()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }

        quiz.getQuestions().clear();
        quiz.getQuestions().addAll(questions);
        return quizMapper.toResponseDTO(quizRepository.save(quiz));
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
