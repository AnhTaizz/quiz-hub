package com.example.quizhub.dto.quiztaking.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponseDTO {
    private Long attemptId;
    private String quizTitle;
    private BigDecimal score;
    private int correctNum;
    private int incorrectNum;
    private int totalNum;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private List<QuestionResultDTO> questions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestionResultDTO {
        private Long questionId;
        private String text;
        private String type;
        private String level;
        private List<AnswerResultDTO> answers;
        private List<Long> selectedAnswerIds;
        private String selectedText;
        @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
        private boolean isCorrect;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResultDTO {
        private Long answerId;
        private String text;
        @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
        private boolean isCorrect;
    }
}
