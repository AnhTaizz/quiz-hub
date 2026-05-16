package com.example.quizhub.service.quiz.impl;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.quizassigning.request.QuizAssigningRequestDTO;
import com.example.quizhub.dto.quizassigning.response.QuizAssigningResponseDTO;
import com.example.quizhub.entity.Classroom;
import com.example.quizhub.entity.Quiz;
import com.example.quizhub.entity.QuizAssigning;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.mapper.QuizAssigningMapper;
import com.example.quizhub.repository.ClassTopicRepository;
import com.example.quizhub.repository.ClassroomRepository;
import com.example.quizhub.repository.QuizAssigningRepository;
import com.example.quizhub.repository.QuizRepository;
import com.example.quizhub.service.quiz.QuizAssigningService;
import com.example.quizhub.service.notification.NotificationService;
import com.example.quizhub.entity.enums.JoinStatus;
import com.example.quizhub.entity.enums.NotificationType;
import com.example.quizhub.repository.ClassJoiningRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizAssigningServiceImpl implements QuizAssigningService {
    private final QuizAssigningRepository quizAssigningRepository;
    private final QuizAssigningMapper quizAssigningMapper;
    private final ClassroomRepository classroomRepository;
    private final QuizRepository quizRepository;
    private final ClassTopicRepository classTopicRepository;
    private final ClassJoiningRepository classJoiningRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private com.example.quizhub.entity.User getCurrentUser() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public QuizAssigningResponseDTO create(QuizAssigningRequestDTO request) {
        com.example.quizhub.entity.User currentUser = getCurrentUser();
        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASSROOM_NOT_FOUND));
        
        if (!classroom.getCreator().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Quiz quiz = quizRepository.findById(request.getQuizId())
                .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        if (!quiz.getCreator().getId().equals(currentUser.getId()) && Boolean.TRUE.equals(quiz.getIsDraft())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        QuizAssigning quizAssigning = quizAssigningMapper.toEntity(request);

        if (quizAssigning.getStartDate() == null) {
            quizAssigning.setStartDate(java.time.LocalDateTime.now());
        }
        if (quizAssigning.getDueDate() == null) {
            quizAssigning.setDueDate(java.time.LocalDateTime.now().plusDays(7));
        }

        // Validation logic
        if (quizAssigning.getDueDate().isBefore(java.time.LocalDateTime.now())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (quizAssigning.getDueDate().isBefore(quizAssigning.getStartDate()) ||
                quizAssigning.getDueDate().isEqual(quizAssigning.getStartDate())) {
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        if (quizAssigning.getDurationInMins() != null) {
            if (quizAssigning.getDurationInMins() <= 0) {
                throw new AppException(ErrorCode.INVALID_DURATION);
            }

            long windowMinutes = java.time.Duration.between(quizAssigning.getStartDate(), quizAssigning.getDueDate())
                    .toMinutes();
            if (quizAssigning.getDurationInMins() > windowMinutes) {
                throw new AppException(ErrorCode.INVALID_DURATION);
            }
        }
        if (quizAssigning.getMaxAttempt() == null || quizAssigning.getMaxAttempt() <= 0) {
            quizAssigning.setMaxAttempt(1);
        }
        if (quizAssigning.getQuestionShuffled() == null) {
            quizAssigning.setQuestionShuffled(false);
        }
        if (quizAssigning.getAnswerShuffled() == null) {
            quizAssigning.setAnswerShuffled(false);
        }
        if (quizAssigning.getShowAnswer() == null) {
            quizAssigning.setShowAnswer(true);
        }

        quizAssigning.setClassroom(classroom);
        quizAssigning.setQuiz(quiz);

        if (request.getTopicId() != null) {
            com.example.quizhub.entity.ClassTopic topic = classTopicRepository.findById(request.getTopicId())
                    .orElseThrow(() -> new AppException(ErrorCode.CLASS_TOPIC_NOT_FOUND));
            quizAssigning.setTopic(topic);
        }

        QuizAssigning savedQuizAssigning = quizAssigningRepository.save(quizAssigning);

        // Notify the teacher themselves for confirmation
        try {
            notificationService.createNotification(
                    classroom.getCreator().getId(),
                    "Đã giao bài kiểm tra",
                    "Bạn đã giao thành công bài kiểm tra \"" + quiz.getTitle() + "\" cho lớp " + classroom.getName(),
                    NotificationType.SYSTEM_ALERT,
                    "/teacher/classrooms/" + classroom.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Notify students in the classroom
        try {
            classJoiningRepository.findByClassroomIdAndStatus(classroom.getId(), JoinStatus.APPROVED)
                    .forEach(joining -> {
                        notificationService.createNotification(
                                joining.getLearner().getId(),
                                "Bài kiểm tra mới: " + quiz.getTitle(),
                                "Bạn có bài kiểm tra mới trong lớp " + classroom.getName(),
                                NotificationType.QUIZ_ASSIGNED,
                                "/student/classrooms/" + classroom.getId());
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }

        return quizAssigningMapper.toResponseDTO(savedQuizAssigning);
    }
}
