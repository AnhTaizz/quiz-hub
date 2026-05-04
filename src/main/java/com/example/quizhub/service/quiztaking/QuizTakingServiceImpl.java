package com.example.quizhub.service.quiztaking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.quiztaking.request.QuestionSubmitRequestDTO;
import com.example.quizhub.dto.quiztaking.request.QuizSubmitRequestDTO;
import com.example.quizhub.dto.quiztaking.response.AnswerTakingResponseDTO;
import com.example.quizhub.dto.quiztaking.response.QuestionTakingResponseDTO;
import com.example.quizhub.dto.quiztaking.response.QuizResultResponseDTO;
import com.example.quizhub.dto.quiztaking.response.QuizTakingResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Attempt;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.Quiz;
import com.example.quizhub.entity.QuizAssigning;
import com.example.quizhub.entity.QuizTaking;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.UserAttemptAnswer;
import com.example.quizhub.entity.enums.TakingStatus;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.repository.AnswerRepository;
import com.example.quizhub.repository.AttemptRepository;
import com.example.quizhub.repository.QuizAssigningRepository;
import com.example.quizhub.repository.QuizTakingRepository;
import com.example.quizhub.repository.UserAttemptAnswerRepository;
import com.example.quizhub.repository.QuizRepository;
import com.example.quizhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizTakingServiceImpl implements QuizTakingService {
    private final UserRepository userRepository;
    private final QuizTakingRepository quizTakingRepository;
    private final AnswerRepository answerRepository;
    private final AttemptRepository attemptRepository;
    private final QuizAssigningRepository quizAssigningRepository;
    private final UserAttemptAnswerRepository userAttemptAnswerRepository;
    private final QuizRepository quizRepository;

    @Override
    @Transactional
    public QuizTakingResponseDTO startQuizAttempt(Long studentId, Long quizAssigningId) {
        User student = userRepository.findById(studentId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        QuizAssigning quizAssigning = quizAssigningRepository.findById(quizAssigningId)
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        Quiz quiz = quizAssigning.getQuiz();
        QuizTaking quizTaking = quizTakingRepository.findByLearnerIdAndQuizAssigningId(studentId, quizAssigningId)
                                    .stream().findFirst()
                                    .orElseGet(() -> quizTakingRepository.save(QuizTaking.builder()
                                                         .isAssigned(true)
                                                         .status(TakingStatus.NOT_STARTED)
                                                         .quiz(quiz)
                                                         .quizAssigning(quizAssigning)
                                                         .learner(student)
                                                         .build()));

        // Find existing attempts
        List<Attempt> attempts = attemptRepository.findByQuizTakingId(quizTaking.getId());
        
        // Find unfinished attempt to resume
        Attempt attempt = attempts.stream()
                .filter(a -> a.getEndedAt() == null)
                .findFirst()
                .orElse(null);

        if (attempt == null) {
            // Check if reached max attempts
            if (quizAssigning.getMaxAttempt() != null) {
                long finishedCount = attempts.stream()
                        .filter(a -> a.getEndedAt() != null)
                        .count();
                if (finishedCount >= quizAssigning.getMaxAttempt()) {
                    throw new AppException(ErrorCode.MAX_ATTEMPTS_REACHED);
                }
            }

            attempt = Attempt.builder()
                .quizTaking(quizTaking)
                .startedAt(LocalDateTime.now())
                .totalQuestNum(quiz.getQuestions().size())
                .build();
            attempt = attemptRepository.save(attempt);
            quizTaking.setStatus(TakingStatus.IN_PROGRESS);
            quizTakingRepository.save(quizTaking);
        }

        java.util.Random random = new java.util.Random(attempt.getId());

        List<QuestionTakingResponseDTO> questionDTOs = quiz.getQuestions().stream()
                    .map(question -> {
                        List<AnswerTakingResponseDTO> answerDTOs = question.getAnswers().stream()
                                                    .map(ans -> new AnswerTakingResponseDTO(
                                                                ans.getId(),
                                                                ans.getText()))
                                                    .collect(Collectors.toList());

                        if(Boolean.TRUE.equals(quizAssigning.getAnswerShuffled())){
                            Collections.shuffle(answerDTOs, random);
                        }

                        return new QuestionTakingResponseDTO(
                            question.getId(),
                            question.getText(),
                            question.getType(),
                            answerDTOs
                        );
                    })
                    .collect(Collectors.toList());

        if(Boolean.TRUE.equals(quizAssigning.getQuestionShuffled())){
            Collections.shuffle(questionDTOs, random);
        }

        // Fetch saved answers for this attempt
        java.util.Map<Long, List<Long>> selectedAnswers = userAttemptAnswerRepository.findByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        uaa -> uaa.getQuestion().getId(),
                        Collectors.mapping(uaa -> uaa.getAnswer().getId(), Collectors.toList())
                ));

        return QuizTakingResponseDTO.builder()
                .attemptId(attempt.getId())
                .quizTitle(quiz.getTitle())
                .durationInMins(quizAssigning.getDurationInMins())
                .startedAt(attempt.getStartedAt())
                .questions(questionDTOs)
                .selectedAnswers(selectedAnswers)
                .build();
    }

    @Override
    @Transactional
    public void saveAnswer(Long studentId, Long attemptId, Long questionId, List<Long> answerIds) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_FOUND));

        if (!attempt.getQuizTaking().getLearner().getId().equals(studentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (attempt.getEndedAt() != null) {
            throw new AppException(ErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }

        // Remove old answers for this question in this attempt
        userAttemptAnswerRepository.deleteByAttemptIdAndQuestionId(attemptId, questionId);

        // Save new answers
        for (Long answerId : answerIds) {
            Answer answer = answerRepository.findById(answerId)
                    .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));
            Question question = Question.builder().id(questionId).build();

            UserAttemptAnswer userAttemptAnswer = UserAttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .answer(answer)
                    .timestamp(LocalDateTime.now())
                    .build();

            userAttemptAnswerRepository.save(userAttemptAnswer);
        }
    }

    @Override
    @Transactional
    public Attempt submitQuizAttempt(Long studentId, QuizSubmitRequestDTO requestDTO) {
        Attempt attempt = attemptRepository.findById(requestDTO.getAttemptId())
                            .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_FOUND));

        if(!attempt.getQuizTaking().getLearner().getId().equals(studentId)){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if(attempt.getEndedAt() != null){
            throw new AppException(ErrorCode.ATTEMPT_ALREADY_SUBMITTED);
        }

        Quiz quiz = attempt.getQuizTaking().getQuiz();
        int totalQuestion = attempt.getTotalQuestNum();
        int correctCount = 0;

        //Map Id câu hỏi và Id đáp án của học sinh
        Map<Long, List<Long>> submittedAnswers = requestDTO.getQuestions().stream()
                                                    .collect(Collectors.toMap(
                                                        QuestionSubmitRequestDTO::getQuestionId,
                                                        QuestionSubmitRequestDTO::getAnswerIds,
                                                        (existing, replacement) -> existing
                                                    ));

        //Chấm điểm từng câu
        for(Question question : quiz.getQuestions()){
            List<Long> submitedAnswersIds = submittedAnswers.getOrDefault(question.getId(), Collections.emptyList());
            List<Long> correctAnswersIds = question.getAnswers().stream()
                                                    .filter(Answer::getIsCorrect)
                                                    .map(Answer::getId)
                                                    .collect(Collectors.toList());

            if(submitedAnswersIds.size() == correctAnswersIds.size() && submitedAnswersIds.containsAll(correctAnswersIds)){
                correctCount++;
            }
            
            // Note: Answers are already saved in real-time via saveAnswer API.
            // We only re-save if the submitted list differs or if we want to ensure final state.
            // For robustness, let's refresh them one last time to match the final submission.
            userAttemptAnswerRepository.deleteByAttemptIdAndQuestionId(attempt.getId(), question.getId());
            for(Long answerId : submitedAnswersIds){
                Answer answer = answerRepository.findById(answerId)
                                    .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

                UserAttemptAnswer userAttemptAnswer = UserAttemptAnswer.builder()
                                                    .attempt(attempt)
                                                    .question(question)
                                                    .answer(answer)
                                                    .timestamp(LocalDateTime.now())
                                                    .build();

                userAttemptAnswerRepository.save(userAttemptAnswer);
            }
        }

        //Tính điểm
        int incorrectCount = totalQuestion - correctCount;
        BigDecimal finalScore = BigDecimal.ZERO;

        if(totalQuestion > 0){
            finalScore = BigDecimal.valueOf((double) correctCount / totalQuestion * 10.0)
                                    .setScale(2, RoundingMode.HALF_UP);
        }

        attempt.setResult(finalScore);
        attempt.setCorrectNum(correctCount);
        attempt.setIncorrectNum(incorrectCount);
        attempt.setEndedAt(LocalDateTime.now());
        attempt.getQuizTaking().setStatus(TakingStatus.COMPLETED);

        attemptRepository.save(attempt);

        return attempt;
    }

    @Override
    public QuizResultResponseDTO getQuizResult(Long studentId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_FOUND));

        if (!attempt.getQuizTaking().getLearner().getId().equals(studentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Quiz quiz = attempt.getQuizTaking().getQuiz();
        QuizAssigning quizAssigning = attempt.getQuizTaking().getQuizAssigning();
        
        // Logical fix: only show answers if (personal quiz) OR (showAnswer is true AND deadline has passed)
        boolean deadlinePassed = quizAssigning == null || (quizAssigning.getDueDate() != null && quizAssigning.getDueDate().isBefore(java.time.LocalDate.now()));
        boolean shouldShowAnswer = quizAssigning == null || (Boolean.TRUE.equals(quizAssigning.getShowAnswer()) && deadlinePassed);

        List<UserAttemptAnswer> userAnswers = userAttemptAnswerRepository.findByAttemptId(attemptId);

        // Map questionId -> list of selected answerIds
        Map<Long, List<Long>> selectedAnswersMap = userAnswers.stream()
                .collect(Collectors.groupingBy(
                        ua -> ua.getQuestion().getId(),
                        Collectors.mapping(ua -> ua.getAnswer().getId(), Collectors.toList())
                ));

        List<QuizResultResponseDTO.QuestionResultDTO> questionResults = quiz.getQuestions().stream()
                .map(q -> {
                    List<Long> selectedIds = selectedAnswersMap.getOrDefault(q.getId(), Collections.emptyList());
                    List<Long> correctIds = q.getAnswers().stream()
                            .filter(Answer::getIsCorrect)
                            .map(Answer::getId)
                            .collect(Collectors.toList());

                    boolean isCorrect = selectedIds.size() == correctIds.size() && selectedIds.containsAll(correctIds);

                    List<QuizResultResponseDTO.AnswerResultDTO> answerResults = q.getAnswers().stream()
                            .map(a -> QuizResultResponseDTO.AnswerResultDTO.builder()
                                    .answerId(a.getId())
                                    .text(a.getText())
                                    .isCorrect(shouldShowAnswer ? a.getIsCorrect() : null)
                                    .build())
                            .collect(Collectors.toList());

                    return QuizResultResponseDTO.QuestionResultDTO.builder()
                            .questionId(q.getId())
                            .text(q.getText())
                            .type(q.getType().name())
                            .answers(answerResults)
                            .selectedAnswerIds(selectedIds)
                            .isCorrect(shouldShowAnswer ? isCorrect : null)
                            .build();
                })
                .collect(Collectors.toList());

        return QuizResultResponseDTO.builder()
                .attemptId(attempt.getId())
                .quizTitle(quiz.getTitle())
                .score(attempt.getResult())
                .correctNum(shouldShowAnswer ? attempt.getCorrectNum() : null)
                .incorrectNum(shouldShowAnswer ? attempt.getIncorrectNum() : null)
                .totalNum(attempt.getTotalQuestNum())
                .startedAt(attempt.getStartedAt())
                .endedAt(attempt.getEndedAt())
                .questions(questionResults)
                .build();
    }

    @Override
    @Transactional
    public QuizTakingResponseDTO startPersonalQuizAttempt(Long studentId, String quizId) {
        User student = userRepository.findById(studentId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Quiz quiz = quizRepository.findById(UUID.fromString(quizId))
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        QuizTaking quizTaking = quizTakingRepository.findByLearnerIdAndQuizIdAndIsAssignedFalse(studentId, UUID.fromString(quizId))
                                    .stream().findFirst()
                                    .orElseGet(() -> quizTakingRepository.save(QuizTaking.builder()
                                                         .isAssigned(false)
                                                         .status(TakingStatus.NOT_STARTED)
                                                         .quiz(quiz)
                                                         .learner(student)
                                                         .build()));

        List<Attempt> attempts = attemptRepository.findByQuizTakingId(quizTaking.getId());
        Attempt attempt = attempts.stream()
                .filter(a -> a.getEndedAt() == null)
                .findFirst()
                .orElse(null);

        if (attempt == null) {
            attempt = Attempt.builder()
                .quizTaking(quizTaking)
                .startedAt(LocalDateTime.now())
                .totalQuestNum(quiz.getQuestions().size())
                .build();
            attempt = attemptRepository.save(attempt);
            quizTaking.setStatus(TakingStatus.IN_PROGRESS);
            quizTakingRepository.save(quizTaking);
        }

        return buildQuizTakingResponseDTO(attempt, quiz, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.example.quizhub.dto.quiztaking.response.QuizAttemptSummaryDTO> getQuizAttempts(Long studentId, String quizId) {
        QuizTaking quizTaking = quizTakingRepository.findByLearnerIdAndQuizIdAndIsAssignedFalse(studentId, UUID.fromString(quizId))
                .stream().findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        return attemptRepository.findByQuizTakingId(quizTaking.getId()).stream()
                .filter(a -> a.getEndedAt() != null)
                .map(a -> com.example.quizhub.dto.quiztaking.response.QuizAttemptSummaryDTO.builder()
                        .id(a.getId())
                        .result(a.getResult())
                        .totalQuestNum(a.getTotalQuestNum())
                        .correctNum(a.getCorrectNum())
                        .incorrectNum(a.getIncorrectNum())
                        .startedAt(a.getStartedAt())
                        .endedAt(a.getEndedAt())
                        .build())
                .sorted((a1, a2) -> a2.getStartedAt().compareTo(a1.getStartedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public QuizTakingResponseDTO getQuizTakingState(Long studentId, Long attemptId) {
        Attempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_FOUND));

        if (!attempt.getQuizTaking().getLearner().getId().equals(studentId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        QuizTaking quizTaking = attempt.getQuizTaking();
        Quiz quiz = quizTaking.getQuiz();
        QuizAssigning quizAssigning = quizTaking.getQuizAssigning();

        return buildQuizTakingResponseDTO(attempt, quiz, quizAssigning);
    }

    private QuizTakingResponseDTO buildQuizTakingResponseDTO(Attempt attempt, Quiz quiz, QuizAssigning quizAssigning) {
        java.util.Random random = new java.util.Random(attempt.getId());

        List<QuestionTakingResponseDTO> questionDTOs = quiz.getQuestions().stream()
                    .map(question -> {
                        List<AnswerTakingResponseDTO> answerDTOs = question.getAnswers().stream()
                                                    .map(ans -> new AnswerTakingResponseDTO(
                                                                ans.getId(),
                                                                ans.getText()))
                                                    .collect(Collectors.toList());

                        if(quizAssigning != null && Boolean.TRUE.equals(quizAssigning.getAnswerShuffled())){
                            Collections.shuffle(answerDTOs, random);
                        }

                        return new QuestionTakingResponseDTO(
                            question.getId(),
                            question.getText(),
                            question.getType(),
                            answerDTOs
                        );
                    })
                    .collect(Collectors.toList());

        if(quizAssigning != null && Boolean.TRUE.equals(quizAssigning.getQuestionShuffled())){
            Collections.shuffle(questionDTOs, random);
        }

        // Fetch saved answers for this attempt
        java.util.Map<Long, List<Long>> selectedAnswers = userAttemptAnswerRepository.findByAttemptId(attempt.getId())
                .stream()
                .collect(Collectors.groupingBy(
                        uaa -> uaa.getQuestion().getId(),
                        Collectors.mapping(uaa -> uaa.getAnswer().getId(), Collectors.toList())
                ));

        return QuizTakingResponseDTO.builder()
                .attemptId(attempt.getId())
                .quizTitle(quiz.getTitle())
                .durationInMins(quizAssigning != null ? quizAssigning.getDurationInMins() : null)
                .startedAt(attempt.getStartedAt())
                .questions(questionDTOs)
                .selectedAnswers(selectedAnswers)
                .build();
    }
}
