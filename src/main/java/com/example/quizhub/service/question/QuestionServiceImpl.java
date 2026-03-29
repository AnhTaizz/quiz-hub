package com.example.quizhub.service.question;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.quizhub.dto.question.request.AnswerCreationRequestDTO;
import com.example.quizhub.dto.question.request.QuestionRequestDTO;
import com.example.quizhub.dto.question.response.QuestionResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.QuestionType;
import com.example.quizhub.mapper.QuestionMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final QuestionMapper questionMapper;

    @Override
    public QuestionResponseDTO createNewQuestion(Long userId, QuestionRequestDTO request) {
        validateQuestionLogic(request.getType(), request.getAnswers());      //ktra logic

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User có id là " + userId));

        Category category = null;
        if(request.getCategoryId() != null){
            category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Category có id là " + request.getCategoryId()));
        }

        //Tạo question entity
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
    public QuestionResponseDTO updateQuestion(Long id, QuestionRequestDTO request) {
        validateQuestionLogic(request.getType(), request.getAnswers());      //ktra logic

        Question question = questionRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy Question có id là " + id));

        Category category = null;
        if(request.getCategoryId() != null){
            category = categoryRepository.findById(request.getCategoryId())
                                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Category có id là " + request.getCategoryId()));
        }

        question.setText(request.getText());
        question.setType(request.getType());
        question.setCategory(category);

        if (request.getIsActive() != null) question.setIsActive(request.getIsActive());
        if (request.getIsPublic() != null) question.setIsPublic(request.getIsPublic());

        List<Answer> newAnswers = request.getAnswers().stream()
                .map(ansDto -> Answer.builder()
                        .text(ansDto.getText())
                        .isCorrect(ansDto.getIsCorrect())
                        .question(question)
                        .build())
                .collect(Collectors.toList());

        question.getAnswers().clear();
        question.getAnswers().addAll(newAnswers);

        Question saved = questionRepository.save(question);
        return questionMapper.toQuestionResponse(saved);
    }

    @Override
    public Page<QuestionResponseDTO> getAllQuestions(Long categoryId, QuestionType type,
                                                    String keyword, int page, int size,
                                                    String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                            ? Sort.by(sortBy).ascending()
                            : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Question> questionPage = questionRepository.findAll(pageable);

        return questionPage.map(questionMapper::toQuestionResponse);
    }

    @Override
    public void deleteQuestion(Long id) {
        Question question = questionRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Không tìm thấy Question có id là " + id));
        question.setIsActive(false);
        questionRepository.save(question);
    }

    // Business Logic
    private void validateQuestionLogic(QuestionType type, List<AnswerCreationRequestDTO> answers) {
        if (answers.size() < 2) {
            throw new RuntimeException("Một câu hỏi trắc nghiệm phải có ít nhất 2 đáp án để lựa chọn.");
        }
        //Số lượng isCorrect = true
        long correctAnswersCount = answers.stream()
                .filter(AnswerCreationRequestDTO::getIsCorrect)
                .count();

        // Ràng buộc theo từng loại câu hỏi
        switch (type) {
            case SINGLE_CHOICE:
                if (correctAnswersCount != 1) {
                    throw new RuntimeException("Câu hỏi trắc nghiệm 1 đáp án phải có CHÍNH XÁC 1 đáp án đúng. Hiện tại đang có " + correctAnswersCount);
                }
                break;
            case MULTIPLE_CHOICE:
                if (correctAnswersCount < 2) {
                    throw new RuntimeException("Câu hỏi trắc nghiệm nhiều đáp án phải có ÍT NHẤT 2 đáp án đúng. Hiện tại đang có " + correctAnswersCount);
                }
                break;
            case FILL_IN_BLANK:
                if (correctAnswersCount < 1) {
                    throw new RuntimeException("Câu hỏi Điền khuyết phải có ít nhất 1 đáp án đúng làm từ khóa.");
                }
                break;
        }
    }
}
