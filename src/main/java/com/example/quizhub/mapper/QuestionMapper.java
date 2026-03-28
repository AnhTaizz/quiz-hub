package com.example.quizhub.mapper;

import com.example.quizhub.dto.response.AnswerResponseDTO;
import com.example.quizhub.dto.response.QuestionResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Question;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class QuestionMapper {

    /**
     * Chuyển Answer entity → AnswerResponseDTO (không có back-reference tới Question).
     */
    public AnswerResponseDTO toAnswerResponse(Answer answer) {
        if (answer == null) return null;
        return AnswerResponseDTO.builder()
                .id(answer.getId())
                .text(answer.getText())
                .isCorrect(answer.getIsCorrect())
                .build();
    }

    /**
     * Chuyển Question entity → QuestionResponseDTO.
     * Không serialize trực tiếp entity để tránh lỗi JSON đệ quy:
     *   Answer.question → Question → Answer (vòng lặp)
     *   Category.parent / Category.children (self-referencing)
     *   User → roles/permissions (dữ liệu không cần thiết)
     */
    public QuestionResponseDTO toQuestionResponse(Question question) {
        if (question == null) return null;

        List<AnswerResponseDTO> answerDTOs = (question.getAnswers() == null)
                                            ? Collections.emptyList()
                                            : question.getAnswers().stream()
                                                    .map(this::toAnswerResponse)
                                                    .collect(Collectors.toList());

        return QuestionResponseDTO.builder()
                .id(question.getId())
                .text(question.getText())
                .type(question.getType())
                .isActive(question.getIsActive())
                .isPublic(question.getIsPublic())
                .creatorId(question.getCreator() != null ? question.getCreator().getId() : null)
                .creatorEmail(question.getCreator() != null ? question.getCreator().getEmail() : null)
                .categoryId(question.getCategory() != null ? question.getCategory().getId() : null)
                .categoryName(question.getCategory() != null ? question.getCategory().getName() : null)
                .answers(answerDTOs)
                .build();
    }

    /**
     * Chuyển danh sách Question entities → danh sách QuestionResponseDTO.
     */
    public List<QuestionResponseDTO> toQuestionResponseList(List<Question> questions) {
        if (questions == null) return Collections.emptyList();
        return questions.stream()
                .map(this::toQuestionResponse)
                .collect(Collectors.toList());
    }
}
