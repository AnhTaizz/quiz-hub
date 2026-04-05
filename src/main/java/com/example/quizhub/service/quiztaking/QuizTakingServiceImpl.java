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

    @Override
    @Transactional
    public QuizTakingResponseDTO startQuizAttempt(Long studentId, Long quizAssigningId) {
        User student = userRepository.findById(studentId)
                        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        QuizAssigning quizAssigning = quizAssigningRepository.findById(quizAssigningId)
                        .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        Quiz quiz = quizAssigning.getQuiz();
        QuizTaking quizTaking = quizTakingRepository.findByLearnerIdAndQuizId(studentId, quiz.getId())
                                    .stream().findFirst()
                                    .orElseGet(() -> QuizTaking.builder()
                                                        .isAssigned(true)
                                                        .status(TakingStatus.IN_PROGRESS)
                                                        .quiz(quiz)
                                                        .learner(student)
                                                        .build());

        Attempt attempt = Attempt.builder()
                .quizTaking(quizTaking)
                .startedAt(LocalDateTime.now())
                .totalQuestNum(quiz.getQuestions().size())
                .build();

        attempt = attemptRepository.save(attempt);

        List<QuestionTakingResponseDTO> questionDTOs = quiz.getQuestions().stream()
                    .map(question -> {
                        List<AnswerTakingResponseDTO> answerDTOs = question.getAnswers().stream()
                                                    .map(ans -> new AnswerTakingResponseDTO(
                                                                ans.getId(),
                                                                ans.getText()))
                                                    .collect(Collectors.toList());

                        if(Boolean.TRUE.equals(quizAssigning.getAnswerShuffled())){
                            Collections.shuffle(answerDTOs);
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
            Collections.shuffle(questionDTOs);
        }

        return QuizTakingResponseDTO.builder()
                .attemptId(attempt.getId())
                .quizTitle(quiz.getTitle())
                .questions(questionDTOs)
                .build();
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

            //Lưu câu trả lời vào DB
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
}
