package com.example.quizhub.service.quiz;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.quiz.QuizRequestDTO;
import com.example.quizhub.dto.quiz.QuizResponseDTO;
import com.example.quizhub.dto.quiz.QuizSummaryDTO;
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

    @Override
    public List<QuizSummaryDTO> getPublicQuizzesByCategoryId(Long categoryId) {
        return quizRepository
                .findByCategoryIdAndIsDraftFalseAndIsEnableTrue(categoryId)
                .stream()
                .map(QuizSummaryDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizSummaryDTO> getMyQuizzesByCategoryId(Long categoryId) {
        Long creatorId = getCurrentUser().getId();
        return quizRepository
                .findByCategoryIdAndCreatorId(categoryId, creatorId)
                .stream()
                .map(QuizSummaryDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuizResponseDTO generateQuizFromCategory(com.example.quizhub.dto.quiz.QuizGenerateRequestDTO request) {
        Category category = resolveCategory(request.getCategoryId());

        List<Long> questionIds = questionRepository.findQuestionIdsByCategoryAndStatus(category.getId(), com.example.quizhub.entity.enums.QuestionStatus.PUBLIC);
        if (questionIds.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND); // No questions available
        }

        List<Long> selectedIds;
        if ("RANDOM".equalsIgnoreCase(request.getMethod())) {
            int amount = request.getAmount() != null ? request.getAmount() : 40;
            java.util.Collections.shuffle(questionIds);
            selectedIds = questionIds.stream().limit(amount).collect(Collectors.toList());
        } else if ("RANGE".equalsIgnoreCase(request.getMethod())) {
            int offset = request.getOffset() != null ? request.getOffset() : 0;
            int limit = request.getLimit() != null ? request.getLimit() : 40;
            selectedIds = questionIds.stream().skip(offset).limit(limit).collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("Invalid method. Must be RANDOM or RANGE");
        }

        if (selectedIds.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }

        List<Question> questions = questionRepository.findAllById(selectedIds);

        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description("Generated from category: " + category.getName())
                .isDraft(false)
                .isEnable(true)
                .isExam(false)
                .category(category)
                .creator(getCurrentUser())
                .questions(questions)
                .build();

        return quizMapper.toResponseDTO(quizRepository.save(quiz));
    }
}
