package com.example.quizhub.service.quiz.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.quiz.request.QuizRequestDTO;
import com.example.quizhub.dto.quiz.request.QuizGenerateRequestDTO;
import com.example.quizhub.dto.quiz.request.BulkQuizCreateRequestDTO;
import com.example.quizhub.dto.question.QuestionRequestDTO;
import com.example.quizhub.service.QuestionService;
import com.example.quizhub.dto.question.QuestionResponseDTO;
import java.util.ArrayList;
import com.example.quizhub.dto.quiz.response.QuizResponseDTO;
import com.example.quizhub.dto.quiz.response.QuizSummaryDTO;
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
import com.example.quizhub.service.quiz.QuizService;
import com.example.quizhub.entity.enums.QuestionStatus;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final QuizMapper quizMapper;
    private final QuestionRepository questionRepository;
    private final com.example.quizhub.repository.QuizTakingRepository quizTakingRepository;
    private final com.example.quizhub.repository.AttemptRepository attemptRepository;
    private final com.example.quizhub.repository.QuizAssigningRepository quizAssigningRepository;
    private final com.example.quizhub.service.CategoryService categoryService;
    private final QuestionService questionService;

    // Helper

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null || categoryId == -1L) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private Category resolveAndValidateCategory(Long categoryId, User currentUser) {
        Category category = resolveCategory(categoryId);
        if (category != null) {
            boolean isAdmin = currentUser.getRole().name().equalsIgnoreCase("ADMIN");
            if (!isAdmin) {
                if (category.getCreator() == null || !category.getCreator().getId().equals(currentUser.getId())) {
                    throw new AppException(ErrorCode.UNAUTHORIZED);
                }
            }
        }
        return category;
    }

    private Quiz findQuiz(String id) {
        return quizRepository.findById(UUID.fromString(id))
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));
    }

    private boolean hasEverBeenAssigned(Quiz quiz) {
        return quizAssigningRepository.countAnyByQuizIdIncludingDeleted(quiz.getId()) > 0;
    }

    private boolean hasSameQuestionMembership(Quiz quiz, List<Long> requestedQuestionIds) {
        if (quiz.getQuestions() == null || requestedQuestionIds == null) {
            return quiz.getQuestions() == null && requestedQuestionIds == null;
        }

        Set<Long> currentQuestionIds = quiz.getQuestions().stream()
                .map(Question::getId)
                .collect(Collectors.toCollection(HashSet::new));
        Set<Long> requestedIds = new HashSet<>(requestedQuestionIds);

        return currentQuestionIds.size() == requestedQuestionIds.size()
                && requestedIds.size() == requestedQuestionIds.size()
                && currentQuestionIds.equals(requestedIds);
    }

    // API

    @Override
    @Transactional
    public QuizResponseDTO createNewQuiz(QuizRequestDTO request) {
        User currentUser = getCurrentUser();
        Quiz quiz = quizMapper.toEntity(request);
        quiz.setCreator(currentUser);
        quiz.setCategory(resolveAndValidateCategory(request.getCategoryId(), currentUser));
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
    @Transactional
    public QuizResponseDTO bulkCreateQuiz(BulkQuizCreateRequestDTO request) {
        User currentUser = getCurrentUser();
        Category category = resolveAndValidateCategory(request.getCategoryId(), currentUser);

        // Phase 1: Batch create all questions and harvest IDs
        List<Long> questionIds = new ArrayList<>();
        for (QuestionRequestDTO qDto : request.getQuestions()) {
            // Force correct category onto raw questions to match parent quiz choice if blank
            if (qDto.getCategoryId() == null && category != null) {
                qDto.setCategoryId(category.getId());
            }
            QuestionResponseDTO savedQ = questionService.createNewQuestion(currentUser.getId(), qDto);
            questionIds.add(savedQ.getId());
        }

        // Phase 2: Create Quiz using standard entity assembly
        Quiz quiz = Quiz.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .category(category)
                .creator(currentUser)
                .isDraft(false)
                .isExam(false)
                .isEnable(true)
                .build();

        List<Question> questions = questionRepository.findAllById(questionIds);
        quiz.setQuestions(questions);

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
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equalsIgnoreCase("ADMIN");

        if(!isAdmin && !quiz.getCreator().getId().equals(currentUser.getId())){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        boolean assignedBefore = hasEverBeenAssigned(quiz);
        if (assignedBefore && !hasSameQuestionMembership(quiz, request.getQuestionIds())) {
            throw new AppException(ErrorCode.QUIZ_STRUCTURE_LOCKED);
        }

        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setImageUrl(request.getImageUrl());
        quiz.setIsDraft(request.getIsDraft());
        quiz.setIsExam(request.getIsExam());
        quiz.setCategory(resolveAndValidateCategory(request.getCategoryId(), currentUser));

        List<Question> questions = questionRepository.findAllById(request.getQuestionIds());
        if (questions.size() != request.getQuestionIds().size()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }

        if (!assignedBefore) {
            quiz.getQuestions().clear();
            quiz.getQuestions().addAll(questions);
        }
        return quizMapper.toResponseDTO(quizRepository.save(quiz));
    }

    @Override
    @Transactional
    public QuizResponseDTO cloneQuiz(String id) {
        Quiz original = findQuiz(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equalsIgnoreCase("ADMIN");

        if (!Boolean.TRUE.equals(original.getIsEnable())) {
            throw new AppException(ErrorCode.QUIZ_DISABLED);
        }

        if (!isAdmin && !original.getCreator().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Quiz clone = Quiz.builder()
                .title((original.getTitle() != null ? original.getTitle() : "Chưa có tiêu đề") + " (bản sao)")
                .description(original.getDescription())
                .imageUrl(original.getImageUrl())
                .isDraft(true)
                .isEnable(true)
                .isExam(original.getIsExam())
                .creator(currentUser)
                .category(original.getCategory())
                .questions(original.getQuestions() != null ? new ArrayList<>(original.getQuestions()) : new ArrayList<>())
                .build();

        return quizMapper.toResponseDTO(quizRepository.save(clone));
    }

    @Override
    @Transactional
    public void deleteQuiz(String id) {
        Quiz quiz = findQuiz(id);
        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole().name().equalsIgnoreCase("ADMIN");

        if(!isAdmin && !quiz.getCreator().getId().equals(currentUser.getId())){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Soft delete
        quiz.setIsEnable(false);
        quizRepository.save(quiz);
    }

    @Override
    public List<QuizSummaryDTO> getPublicQuizzesByCategoryId(Long categoryId) {
        List<Long> allIds = categoryService.getAllDescendantIds(categoryId);
        return quizRepository
                .findByCategoryIdInAndIsDraftFalseAndIsEnableTrue(allIds)
                .stream()
                .map(QuizSummaryDTO::new)
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizSummaryDTO> getMyQuizzesByCategoryId(Long categoryId) {
        User user = getCurrentUser();
        List<Long> allIds = categoryService.getAllDescendantIds(categoryId);
        return quizRepository
                .findByCategoryIdInAndCreatorIdAndIsEnableTrue(allIds, user.getId())
                .stream()
                .map(q -> mapToSummaryDTO(q, user))
                .collect(Collectors.toList());
    }

    @Override
    public List<QuizSummaryDTO> getMyQuizzes() {
        User user = getCurrentUser();
        return quizRepository
                .findByCreatorIdAndIsEnableTrue(user.getId())
                .stream()
                .map(q -> mapToSummaryDTO(q, user))
                .collect(Collectors.toList());
    }

    private QuizSummaryDTO mapToSummaryDTO(Quiz quiz, User user) {
        QuizSummaryDTO dto = new QuizSummaryDTO(quiz);

        // Find personal taking info (where quizAssigning is null)
        quizTakingRepository.findByLearnerIdAndQuizIdAndQuizAssigningIsNull(user.getId(), quiz.getId())
                .stream().findFirst().ifPresent(qt -> {
                    dto.setTakingStatus(qt.getStatus().name());
                    long attempts = attemptRepository.countByQuizTakingIdAndEndedAtIsNotNull(qt.getId());
                    dto.setAttemptInfo("Lần làm: " + attempts);
                });

        return dto;
    }

    @Override
    @Transactional
    public QuizResponseDTO generateQuizFromCategory(QuizGenerateRequestDTO request) {
        Category category = resolveCategory(request.getCategoryId());
        List<Long> allIds = categoryService.getAllDescendantIds(request.getCategoryId());

        List<Long> questionIds = questionRepository.findQuestionIdsByCategoryInAndStatus(allIds, QuestionStatus.PUBLIC);
        if (questionIds.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND); // No questions available
        }

        List<Long> selectedIds;
        if ("RANDOM".equalsIgnoreCase(request.getMethod())) {
            int amount = request.getAmount() != null ? request.getAmount() : 40;
            Collections.shuffle(questionIds);
            selectedIds = questionIds.stream().limit(amount).collect(Collectors.toList());
        } else if ("RANGE".equalsIgnoreCase(request.getMethod())) {
            int offset = request.getOffset() != null ? request.getOffset() : 0;
            int limit = request.getLimit() != null ? request.getLimit() : 40;
            selectedIds = questionIds.stream().skip(offset).limit(limit).collect(Collectors.toList());
        } else {
            throw new AppException(ErrorCode.INVALID_GENERATION_METHOD);
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
                .category(null)
                .creator(getCurrentUser())
                .questions(questions)
                .build();

        return quizMapper.toResponseDTO(quizRepository.save(quiz));
    }
}
