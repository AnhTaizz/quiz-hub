package com.example.quizhub.service.quiztaking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.repository.ExamViolationRepository;
import com.example.quizhub.repository.AttemptViolationRepository;
import com.example.quizhub.entity.AttemptViolation;
import com.example.quizhub.entity.ExamViolation;
import com.example.quizhub.dto.quiztaking.request.ViolationRequestDTO;

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
    private final ExamViolationRepository examViolationRepository;
    private final AttemptViolationRepository attemptViolationRepository;

    @Override
    @Transactional
    public QuizTakingResponseDTO startQuizAttempt(Long studentId, Long quizAssigningId) {
        User student = userRepository.findById(studentId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        QuizAssigning quizAssigning = quizAssigningRepository.findById(quizAssigningId)
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        Quiz quiz = quizAssigning.getQuiz();
        QuizTaking quizTaking = quizTakingRepository.findByLearnerIdAndQuizAssigningId(studentId, quizAssigningId)
                                    .orElseGet(() -> quizTakingRepository.save(QuizTaking.builder()
                                                         .isAssigned(true)
                                                         .status(TakingStatus.NOT_STARTED)
                                                         .quiz(quiz)
                                                         .quizAssigning(quizAssigning)
                                                         .learner(student)
                                                         .build()));

        // Check quiz schedule
        LocalDateTime now = LocalDateTime.now();
        if (quizAssigning.getStartDate() != null && now.isBefore(quizAssigning.getStartDate())) {
            throw new AppException(ErrorCode.QUIZ_NOT_STARTED);
        }
        if (quizAssigning.getDueDate() != null && now.isAfter(quizAssigning.getDueDate())) {
            throw new AppException(ErrorCode.QUIZ_EXPIRED);
        }

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
                            question.getLevel(),
                            answerDTOs
                        );
                    })
                    .collect(Collectors.toList());

        if(Boolean.TRUE.equals(quizAssigning.getQuestionShuffled())){
            Collections.shuffle(questionDTOs, random);
        }

        // Fetch saved answers for this attempt
        List<UserAttemptAnswer> savedAnswers = userAttemptAnswerRepository.findByAttemptId(attempt.getId());
        
        java.util.Map<Long, List<Long>> selectedAnswers = savedAnswers.stream()
                .filter(uaa -> uaa.getAnswer() != null)
                .collect(Collectors.groupingBy(
                        uaa -> uaa.getQuestion().getId(),
                        Collectors.mapping(uaa -> uaa.getAnswer().getId(), Collectors.toList())
                ));

        java.util.Map<Long, String> selectedTexts = savedAnswers.stream()
                .filter(uaa -> uaa.getSelectedText() != null)
                .collect(Collectors.toMap(
                        uaa -> uaa.getQuestion().getId(),
                        UserAttemptAnswer::getSelectedText,
                        (existing, replacement) -> existing
                ));

        return QuizTakingResponseDTO.builder()
                .attemptId(attempt.getId())
                .quizTitle(quiz.getTitle())
                .durationInMins(quizAssigning.getDurationInMins())
                .startedAt(attempt.getStartedAt())
                .startedAtMillis(attempt.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
                .questions(questionDTOs)
                .selectedAnswers(selectedAnswers)
                .selectedTexts(selectedTexts)
                .build();
    }

    @Override
    @Transactional
    public void saveAnswer(Long studentId, Long attemptId, Long questionId, com.example.quizhub.dto.quiztaking.request.SaveAnswerRequestDTO request) {
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

        Question question = Question.builder().id(questionId).build();

        if (request.getSelectedText() != null) {
            UserAttemptAnswer uaa = UserAttemptAnswer.builder()
                    .attempt(attempt)
                    .question(question)
                    .selectedText(request.getSelectedText())
                    .timestamp(LocalDateTime.now())
                    .build();
            userAttemptAnswerRepository.save(uaa);
        } else if (request.getAnswerIds() != null) {
            for (Long answerId : request.getAnswerIds()) {
                Answer answer = answerRepository.findById(answerId)
                        .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

                UserAttemptAnswer uaa = UserAttemptAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .answer(answer)
                        .timestamp(LocalDateTime.now())
                        .build();
                userAttemptAnswerRepository.save(uaa);
            }
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

        //Map Id câu hỏi và DTO của học sinh
        Map<Long, QuestionSubmitRequestDTO> submittedAnswersMap = requestDTO.getQuestions().stream()
                                                    .collect(Collectors.toMap(
                                                        QuestionSubmitRequestDTO::getQuestionId,
                                                        q -> q,
                                                        (existing, replacement) -> existing
                                                    ));

        //Chấm điểm từng câu
        for(Question question : quiz.getQuestions()){
            QuestionSubmitRequestDTO qReq = submittedAnswersMap.get(question.getId());
            
            if (question.getType() == com.example.quizhub.entity.enums.QuestionType.FILL_IN_BLANK) {
                String studentText = (qReq != null ? qReq.getSelectedText() : "");
                boolean isCorrect = question.getAnswers().stream()
                        .filter(Answer::getIsCorrect)
                        .anyMatch(a -> a.getText().trim().equalsIgnoreCase(studentText.trim()));
                
                if (isCorrect) correctCount++;
                
                userAttemptAnswerRepository.deleteByAttemptIdAndQuestionId(attempt.getId(), question.getId());
                userAttemptAnswerRepository.save(UserAttemptAnswer.builder()
                        .attempt(attempt)
                        .question(question)
                        .selectedText(studentText)
                        .timestamp(LocalDateTime.now())
                        .build());
            } else {
                List<Long> submitedAnswersIds = (qReq != null && qReq.getAnswerIds() != null) ? qReq.getAnswerIds() : Collections.emptyList();
                List<Long> correctAnswersIds = question.getAnswers().stream()
                                                        .filter(Answer::getIsCorrect)
                                                        .map(Answer::getId)
                                                        .collect(Collectors.toList());

                if(submitedAnswersIds.size() == correctAnswersIds.size() && submitedAnswersIds.containsAll(correctAnswersIds)){
                    correctCount++;
                }
                
                userAttemptAnswerRepository.deleteByAttemptIdAndQuestionId(attempt.getId(), question.getId());
                for(Long answerId : submitedAnswersIds){
                    Answer answer = answerRepository.findById(answerId)
                                        .orElseThrow(() -> new AppException(ErrorCode.ANSWER_NOT_FOUND));

                    userAttemptAnswerRepository.save(UserAttemptAnswer.builder()
                                                        .attempt(attempt)
                                                        .question(question)
                                                        .answer(answer)
                                                        .timestamp(LocalDateTime.now())
                                                        .build());
                }
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
        List<UserAttemptAnswer> userAnswers = userAttemptAnswerRepository.findByAttemptId(attemptId);

        // Map questionId -> list of selected answerIds
        Map<Long, List<Long>> selectedAnswersMap = userAnswers.stream()
                .filter(ua -> ua.getAnswer() != null)
                .collect(Collectors.groupingBy(
                        ua -> ua.getQuestion().getId(),
                        Collectors.mapping(ua -> ua.getAnswer().getId(), Collectors.toList())
                ));

        QuizAssigning assigning = attempt.getQuizTaking().getQuizAssigning();
        java.util.Random random = new java.util.Random(attempt.getId());

        List<QuizResultResponseDTO.QuestionResultDTO> questionResults = quiz.getQuestions().stream()
                .map(q -> {
                    List<Long> selectedIds = selectedAnswersMap.getOrDefault(q.getId(), Collections.emptyList());
                    String selectedText = userAnswers.stream()
                            .filter(ua -> ua.getQuestion().getId().equals(q.getId()) && ua.getSelectedText() != null)
                            .map(UserAttemptAnswer::getSelectedText)
                            .findFirst().orElse(null);

                    boolean isCorrect = false;
                    if (q.getType() == com.example.quizhub.entity.enums.QuestionType.FILL_IN_BLANK) {
                        isCorrect = q.getAnswers().stream()
                                .filter(Answer::getIsCorrect)
                                .anyMatch(a -> a.getText().trim().equalsIgnoreCase((selectedText != null ? selectedText : "").trim()));
                    } else {
                        List<Long> correctIds = q.getAnswers().stream()
                                .filter(Answer::getIsCorrect)
                                .map(Answer::getId)
                                .collect(Collectors.toList());
                        isCorrect = selectedIds.size() == correctIds.size() && selectedIds.containsAll(correctIds);
                    }

                    List<QuizResultResponseDTO.AnswerResultDTO> answerResults = q.getAnswers().stream()
                            .map(a -> QuizResultResponseDTO.AnswerResultDTO.builder()
                                    .answerId(a.getId())
                                    .text(a.getText())
                                    .isCorrect(a.getIsCorrect())
                                    .build())
                            .collect(Collectors.toList());

                    // Shuffle answers if needed
                    if (assigning != null && Boolean.TRUE.equals(assigning.getAnswerShuffled())) {
                        java.util.Collections.shuffle(answerResults, random);
                    }

                    return QuizResultResponseDTO.QuestionResultDTO.builder()
                            .questionId(q.getId())
                            .text(q.getText())
                            .type(q.getType().name())
                            .level(q.getLevel() != null ? q.getLevel().name() : "MEDIUM")
                            .answers(answerResults)
                            .selectedAnswerIds(selectedIds)
                            .selectedText(selectedText)
                            .isCorrect(isCorrect)
                            .build();
                })
                .collect(Collectors.toList());

        // Shuffle questions if needed
        if (assigning != null && Boolean.TRUE.equals(assigning.getQuestionShuffled())) {
            java.util.Collections.shuffle(questionResults, random);
        }

        return QuizResultResponseDTO.builder()
                .attemptId(attempt.getId())
                .quizTitle(quiz.getTitle())
                .score(attempt.getResult())
                .correctNum(attempt.getCorrectNum())
                .incorrectNum(attempt.getIncorrectNum())
                .totalNum(attempt.getTotalQuestNum())
                .startedAt(attempt.getStartedAt())
                .endedAt(attempt.getEndedAt())
                .questions(questionResults)
                .build();
    }

    @Override
    @Transactional
    public Attempt recordViolation(ViolationRequestDTO request) {
        Attempt attempt = attemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new AppException(ErrorCode.ATTEMPT_NOT_FOUND));

        if (attempt.getEndedAt() != null) {
            return attempt;
        }

        ExamViolation violationType = examViolationRepository.findByViolationCode(request.getViolationCode())
                .orElseGet(() -> examViolationRepository.save(ExamViolation.builder()
                        .violationCode(request.getViolationCode())
                        .severityLevel(1)
                        .description("Tự động tạo cho mã: " + request.getViolationCode())
                        .build()));

        AttemptViolation violation = AttemptViolation.builder()
                .attempt(attempt)
                .violationType(violationType)
                .occurredAt(LocalDateTime.now())
                .build();

        attemptViolationRepository.save(violation);
        return attempt;
    }
}
