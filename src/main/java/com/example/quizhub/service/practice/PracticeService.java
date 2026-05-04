package com.example.quizhub.service.practice;

import java.util.List;

import com.example.quizhub.dto.practice.PracticeHistoryResponseDTO;
import com.example.quizhub.dto.practice.PracticeQuestionResponseDTO;
import com.example.quizhub.dto.practice.PracticeResultResponseDTO;
import com.example.quizhub.dto.practice.PracticeStartRequestDTO;
import com.example.quizhub.dto.practice.PracticeStartResponseDTO;
import com.example.quizhub.dto.practice.PracticeSubmitRequestDTO;

public interface PracticeService {

    List<PracticeQuestionResponseDTO> startPractice(PracticeStartRequestDTO request);

    List<PracticeQuestionResponseDTO> previewPractice(PracticeStartRequestDTO request);

    PracticeResultResponseDTO submitPractice(PracticeSubmitRequestDTO request);

    long countQuestions(Long categoryId);

    List<PracticeHistoryResponseDTO> getPracticeHistory(Long categoryId);

    List<PracticeHistoryResponseDTO> getMyPracticeHistory();

    PracticeResultResponseDTO getPracticeDetail(Long practiceId);

    PracticeStartResponseDTO startPracticeFromQuiz(String quizId);
}

