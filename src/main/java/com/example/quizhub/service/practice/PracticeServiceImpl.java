package com.example.quizhub.service.practice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.practice.PracticeAnswerRequestDTO;
import com.example.quizhub.dto.practice.PracticeAnswerResponseDTO;
import com.example.quizhub.dto.practice.PracticeDetailResponseDTO;
import com.example.quizhub.dto.practice.PracticeQuestionResponseDTO;
import com.example.quizhub.dto.practice.PracticeResultResponseDTO;
import com.example.quizhub.dto.practice.PracticeStartRequestDTO;
import com.example.quizhub.dto.practice.PracticeStartResponseDTO;
import com.example.quizhub.dto.practice.PracticeSubmitRequestDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Practice;
import com.example.quizhub.entity.PracticeDetail;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.Quiz;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.repository.AnswerRepository;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.PracticeDetailRepository;
import com.example.quizhub.repository.PracticeRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.QuizRepository;
import com.example.quizhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private final PracticeRepository practiceRepository;
    private final PracticeDetailRepository practiceDetailRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void collectCategoryIds(Category category, List<Long> ids) {
        ids.add(category.getId());
        if (category.getChildren() != null) {
            for (Category child : category.getChildren()) {
                collectCategoryIds(child, ids);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeQuestionResponseDTO> startPractice(PracticeStartRequestDTO request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        List<Long> categoryIds = new ArrayList<>();
        collectCategoryIds(category, categoryIds);

        // Fetch random questions for these categories
        List<Question> questions = questionRepository.findRandomPublicQuestionsByCategories(
                categoryIds,
                request.getLimit() != null ? request.getLimit() : 10,
                request.getOffset() != null ? request.getOffset() : 0);

        if (questions.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }

        // Map to safe DTO without correct answers
        return questions.stream().map(q -> {
            List<PracticeAnswerResponseDTO> answers = q.getAnswers().stream()
                    .map(a -> new PracticeAnswerResponseDTO(a.getId(), a.getText(), a.getIsCorrect()))
                    .collect(Collectors.toList());

            return PracticeQuestionResponseDTO.builder()
                    .id(q.getId())
                    .text(q.getText())
                    .type(q.getType())
                    .level(q.getLevel())
                    .answers(answers)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PracticeQuestionResponseDTO> previewPractice(PracticeStartRequestDTO request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        List<Long> categoryIds = new ArrayList<>();
        collectCategoryIds(category, categoryIds);

        List<Question> questions = questionRepository.findRandomPublicQuestionsByCategories(
                categoryIds,
                request.getLimit() != null ? request.getLimit() : 10,
                request.getOffset() != null ? request.getOffset() : 0);

        if (questions.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }

        // Map with isCorrect for preview
        return questions.stream().map(q -> {
            List<PracticeAnswerResponseDTO> answers = q.getAnswers().stream()
                    .map(a -> new PracticeAnswerResponseDTO(a.getId(), a.getText(), a.getIsCorrect()))
                    .collect(Collectors.toList());

            return PracticeQuestionResponseDTO.builder()
                    .id(q.getId())
                    .text(q.getText())
                    .type(q.getType())
                    .level(q.getLevel())
                    .answers(answers)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PracticeResultResponseDTO submitPractice(PracticeSubmitRequestDTO request) {
        User user = getCurrentUser();
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        int correctCount = 0;
        int totalQuestions = request.getAnswers().size();

        // Prepare the parent entity
        Practice practice = Practice.builder()
                .user(user)
                .category(category)
                .totalQuestions(totalQuestions)
                .correctAnswers(0)
                .build();

        Practice savedPractice = practiceRepository.save(practice);
        List<PracticeDetailResponseDTO> detailResponses = new ArrayList<>();

        for (PracticeAnswerRequestDTO ansReq : request.getAnswers()) {
            Question question = questionRepository.findById(ansReq.getQuestionId())
                    .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

            Answer selectedAnswer = null;
            boolean isCorrect = false;
            Long correctAnswerId = null;

            // Find the correct answer for this question
            for (Answer a : question.getAnswers()) {
                if (Boolean.TRUE.equals(a.getIsCorrect())) {
                    correctAnswerId = a.getId();
                    break;
                }
            }

            if (ansReq.getSelectedAnswerId() != null) {
                selectedAnswer = answerRepository.findById(ansReq.getSelectedAnswerId()).orElse(null);
                if (selectedAnswer != null && Boolean.TRUE.equals(selectedAnswer.getIsCorrect())) {
                    isCorrect = true;
                    correctCount++;
                }
            }

            // Save detail
            PracticeDetail detail = PracticeDetail.builder()
                    .practice(savedPractice)
                    .question(question)
                    .selectedAnswer(selectedAnswer)
                    .isCorrect(isCorrect)
                    .build();
            practiceDetailRepository.save(detail);

            // Add to response
            detailResponses.add(PracticeDetailResponseDTO.builder()
                    .questionId(question.getId())
                    .questionText(question.getText())
                    .selectedAnswerId(selectedAnswer != null ? selectedAnswer.getId() : null)
                    .correctAnswerId(correctAnswerId)
                    .isCorrect(isCorrect)
                    .answers(question.getAnswers().stream()
                            .map(a -> new PracticeAnswerResponseDTO(a.getId(), a.getText(), a.getIsCorrect()))
                            .collect(Collectors.toList()))
                    .build());
        }

        savedPractice.setCorrectAnswers(correctCount);
        practiceRepository.save(savedPractice);

        return PracticeResultResponseDTO.builder()
                .practiceId(savedPractice.getId())
                .categoryName(category.getName())
                .totalQuestions(totalQuestions)
                .correctAnswers(correctCount)
                .score(BigDecimal.valueOf((correctCount * 10.0) / totalQuestions)
                        .setScale(1, java.math.RoundingMode.HALF_UP))
                .createdAt(savedPractice.getCreatedAt())
                .details(detailResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public long countQuestions(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        List<Long> categoryIds = new ArrayList<>();
        collectCategoryIds(category, categoryIds);

        return questionRepository.countPublicQuestionsByCategories(categoryIds, QuestionStatus.PUBLIC);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.quizhub.dto.practice.PracticeHistoryResponseDTO> getPracticeHistory(Long categoryId) {
        User user = getCurrentUser();
        List<Practice> practices = practiceRepository.findByUserIdAndCategoryIdOrderByCreatedAtDesc(user.getId(),
                categoryId);

        return practices.stream().map(p -> com.example.quizhub.dto.practice.PracticeHistoryResponseDTO.builder()
                .id(p.getId())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .totalQuestions(p.getTotalQuestions())
                .correctAnswers(p.getCorrectAnswers())
                .createdAt(p.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.quizhub.dto.practice.PracticeHistoryResponseDTO> getMyPracticeHistory() {
        User user = getCurrentUser();
        List<Practice> practices = practiceRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return practices.stream().map(p -> com.example.quizhub.dto.practice.PracticeHistoryResponseDTO.builder()
                .id(p.getId())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : "Đề thi cá nhân")
                .totalQuestions(p.getTotalQuestions())
                .correctAnswers(p.getCorrectAnswers())
                .createdAt(p.getCreatedAt())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeResultResponseDTO getPracticeDetail(Long practiceId) {
        User user = getCurrentUser();
        Practice practice = practiceRepository.findById(practiceId)
                .orElseThrow(() -> new AppException(ErrorCode.PRACTICE_NOT_FOUND));

        // Check ownership
        if (!practice.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        List<PracticeDetailResponseDTO> details = practice.getDetails().stream()
                .map(d -> PracticeDetailResponseDTO.builder()
                        .questionId(d.getQuestion().getId())
                        .questionText(d.getQuestion().getText())
                        .selectedAnswerId(d.getSelectedAnswer() != null ? d.getSelectedAnswer().getId() : null)
                        .correctAnswerId(d.getQuestion().getAnswers().stream()
                                .filter(a -> Boolean.TRUE.equals(a.getIsCorrect()))
                                .map(Answer::getId)
                                .findFirst().orElse(null))
                        .isCorrect(d.getIsCorrect())
                        .answers(d.getQuestion().getAnswers().stream()
                                .map(a -> new PracticeAnswerResponseDTO(a.getId(), a.getText(), a.getIsCorrect()))
                                .collect(Collectors.toList()))
                        .build())
                .collect(Collectors.toList());

        return PracticeResultResponseDTO.builder()
                .practiceId(practice.getId())
                .categoryName(practice.getCategory().getName())
                .totalQuestions(practice.getTotalQuestions())
                .correctAnswers(practice.getCorrectAnswers())
                .score(BigDecimal.valueOf((practice.getCorrectAnswers() * 10.0) / practice.getTotalQuestions())
                        .setScale(1, java.math.RoundingMode.HALF_UP))
                .createdAt(practice.getCreatedAt())
                .details(details)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PracticeStartResponseDTO startPracticeFromQuiz(String quizId) {
        Quiz quiz = quizRepository.findById(UUID.fromString(quizId))
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        List<Question> questions = quiz.getQuestions();

        if (questions.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_NOT_FOUND);
        }

        List<PracticeQuestionResponseDTO> questionDTOs = questions.stream().map(q -> {
            List<PracticeAnswerResponseDTO> answers = q.getAnswers().stream()
                    .map(a -> new PracticeAnswerResponseDTO(a.getId(), a.getText(), a.getIsCorrect()))
                    .collect(Collectors.toList());

            return PracticeQuestionResponseDTO.builder()
                    .id(q.getId())
                    .text(q.getText())
                    .type(q.getType())
                    .level(q.getLevel())
                    .answers(answers)
                    .build();
        }).collect(Collectors.toList());

        return com.example.quizhub.dto.practice.PracticeStartResponseDTO.builder()
                .questions(questionDTOs)
                .categoryId(quiz.getCategory() != null ? quiz.getCategory().getId() : null)
                .categoryName(quiz.getCategory() != null ? quiz.getCategory().getName() : "Đề thi cá nhân")
                .quizTitle(quiz.getTitle())
                .build();
    }
}
