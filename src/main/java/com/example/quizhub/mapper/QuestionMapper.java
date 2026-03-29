package com.example.quizhub.mapper;

import com.example.quizhub.dto.question.response.AnswerResponseDTO;
import com.example.quizhub.dto.question.response.QuestionResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Question;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface QuestionMapper {

    // Answer -> AnswerResponseDTO (các field trùng tên, MapStruct tự map)
    AnswerResponseDTO toAnswerResponse(Answer answer);

    // Question -> QuestionResponseDTO
    @Mapping(source = "creator.id",       target = "creatorId")
    @Mapping(source = "creator.email",    target = "creatorEmail")
    @Mapping(source = "category.id",      target = "categoryId")
    @Mapping(source = "category.name",    target = "categoryName")
    QuestionResponseDTO toQuestionResponse(Question question);

    // List<Question> -> List<QuestionResponseDTO>
    List<QuestionResponseDTO> toQuestionResponseList(List<Question> questions);
}
