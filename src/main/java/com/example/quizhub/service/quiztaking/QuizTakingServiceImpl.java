package com.example.quizhub.service.quiztaking;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.quiztaking.response.AnswerTakingResponseDTO;
import com.example.quizhub.dto.quiztaking.response.QuestionTakingResponseDTO;
import com.example.quizhub.dto.quiztaking.response.QuizTakingResponseDTO;
import com.example.quizhub.entity.Attempt;
import com.example.quizhub.entity.Quiz;
import com.example.quizhub.entity.QuizAssigning;
import com.example.quizhub.entity.QuizTaking;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.TakingStatus;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.repository.AnswerRepository;
import com.example.quizhub.repository.AttemptRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.QuizAssigningRepository;
import com.example.quizhub.repository.QuizRepository;
import com.example.quizhub.repository.QuizTakingRepository;
import com.example.quizhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizTakingServiceImpl implements QuizTakingService {
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final QuizTakingRepository quizTakingRepository;
    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AttemptRepository attemptRepository;
    private final QuizAssigningRepository quizAssigningRepository;

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

}
