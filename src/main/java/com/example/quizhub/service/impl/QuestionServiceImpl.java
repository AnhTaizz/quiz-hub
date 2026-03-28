package com.example.quizhub.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.request.QuestionCreationRequestDTO;
import com.example.quizhub.dto.response.QuestionResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.User;
import com.example.quizhub.mapper.QuestionMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.QuestionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionMapper questionMapper;

    @Override
    public QuestionResponseDTO createNewQuestion(Long userId, QuestionCreationRequestDTO request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User có id là " + userId));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category có id là " + request.getCategoryId()));

        Question question = Question.builder()
                .type(request.getType())
                .text(request.getText())
                .isActive(true)
                .isPublic(true)
                .creator(creator)
                .category(category)
                .build();

        List<Answer> answers = request.getAnswers().stream()
                .map(ansDto -> Answer.builder()
                        .text(ansDto.getText())
                        .isCorrect(ansDto.getIsCorrect())
                        .question(question)
                        .build())
                .collect(Collectors.toList());

        question.setAnswers(answers);

        Question saved = questionRepository.save(question);
        return questionMapper.toQuestionResponse(saved);
    }

    @Override
    public QuestionResponseDTO updateQuestion(Long id, QuestionCreationRequestDTO request) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QuestionResponseDTO getQuestionById(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void deleteQuestion(Long id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<QuestionResponseDTO> getAllQuestions() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
