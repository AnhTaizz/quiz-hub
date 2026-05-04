package com.example.quizhub.dto.practice;

import java.util.List;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PracticeDetailResponseDTO {
    Long questionId;
    String questionText;
    List<Long> selectedAnswerIds;
    String selectedText;
    List<Long> correctAnswerIds;
    List<String> correctTexts;
    @com.fasterxml.jackson.annotation.JsonProperty("isCorrect")
    Boolean isCorrect;
    String questionType;
    String questionLevel;
    List<PracticeAnswerResponseDTO> answers;
}
