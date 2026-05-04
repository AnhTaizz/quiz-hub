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
    Long selectedAnswerId;
    Long correctAnswerId; // Send this back in results
    Boolean isCorrect;
    List<PracticeAnswerResponseDTO> answers;
}
