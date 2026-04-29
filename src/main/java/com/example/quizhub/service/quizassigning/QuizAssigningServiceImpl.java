package com.example.quizhub.service.quizassigning;

import java.time.LocalDate;

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

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuizAssigningServiceImpl implements QuizAssigningService {
    private final QuizAssigningRepository quizAssigningRepository;
    private final QuizAssigningMapper quizAssigningMapper;
    private final ClassroomRepository classroomRepository;
    private final QuizRepository quizRepository;
    private final ClassTopicRepository classTopicRepository;

    @Override
    public QuizAssigningResponseDTO create(QuizAssigningRequestDTO request) {
        Classroom classroom = classroomRepository.findById(request.getClassroomId())
                            .orElseThrow(() -> new AppException(ErrorCode.CLASSROOM_NOT_FOUND));
        Quiz quiz = quizRepository.findById(request.getQuizId())
                            .orElseThrow(() -> new AppException(ErrorCode.QUIZ_NOT_FOUND));

        QuizAssigning quizAssigning = quizAssigningMapper.toEntity(request);
        if(quizAssigning.getStartDate() == null){
            quizAssigning.setStartDate(LocalDate.now());
        }
        if(quizAssigning.getDueDate() == null){
            quizAssigning.setDueDate(LocalDate.now().plusDays(7));
        }
        if(quizAssigning.getMaxAttempt() == null){
            quizAssigning.setMaxAttempt(1);
        }
        if(quizAssigning.getQuestionShuffled() == null){
            quizAssigning.setQuestionShuffled(false);
        }
        if(quizAssigning.getAnswerShuffled() == null){
            quizAssigning.setAnswerShuffled(false);
        }

        quizAssigning.setClassroom(classroom);
        quizAssigning.setQuiz(quiz);

        if (request.getTopicId() != null) {
            com.example.quizhub.entity.ClassTopic topic = classTopicRepository.findById(request.getTopicId())
                .orElseThrow(() -> new AppException(ErrorCode.CLASS_TOPIC_NOT_FOUND));
            quizAssigning.setTopic(topic);
        }

        QuizAssigning savedQuizAssigning = quizAssigningRepository.save(quizAssigning);
        return quizAssigningMapper.toResponseDTO(savedQuizAssigning);
    }
}

