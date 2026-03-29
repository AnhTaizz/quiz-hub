package com.example.quizhub.dto.questiontaking;

import java.util.List;

import com.example.quizhub.entity.enums.QuestionType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionTakingDTO {
    Long id;
    QuestionType type;
    String text;
    List<AnswerTakingDTO> answers;
}
