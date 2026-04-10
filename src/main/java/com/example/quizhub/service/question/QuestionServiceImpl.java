package com.example.quizhub.service.question;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.question.request.AnswerCreationRequestDTO;
import com.example.quizhub.dto.question.request.QuestionRequestDTO;
import com.example.quizhub.dto.question.response.QuestionResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.entity.enums.QuestionType;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
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
    @Transactional
    public QuestionResponseDTO createNewQuestion(Long userId, QuestionRequestDTO request) {
        validateQuestionLogic(request.getType(), request.getAnswers());      //ktra logic

        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Category category = null;
        if(request.getCategoryId() != null){
            category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        //Tạo question entity
        Question question = Question.builder()
                            .type(request.getType())
                            .text(request.getText())
                            .isActive(true)
                            .questionStatus(request.getQuestionStatus())
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
        return questionMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public QuestionResponseDTO updateQuestion(Long userId, Long id, QuestionRequestDTO request) {
        validateQuestionLogic(request.getType(), request.getAnswers());      //ktra logic

        Question oldQuestion = questionRepository.findById(id)
                            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        if(!oldQuestion.getCreator().getId().equals(userId)){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        Category category = null;
        if(request.getCategoryId() != null){
            category = categoryRepository.findById(request.getCategoryId())
                                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        boolean isUsedInQuiz = questionRepository.isQuestionUsedInQuiz(id);

        if(isUsedInQuiz){
            oldQuestion.setIsActive(false);
            questionRepository.save(oldQuestion);
            Question cloneQuestion = Question.builder()
                                        .type(request.getType())
                                        .text(request.getText())
                                        .creator(oldQuestion.getCreator())
                                        .category(category)
                                        .isActive(true)
                                        .questionStatus(request.getQuestionStatus())
                                        .build();

            List<Answer> cloneAnswers = request.getAnswers().stream()
                                        .map(ansDto -> Answer.builder()
                                                .text(ansDto.getText())
                                                .isCorrect(ansDto.getIsCorrect())
                                                .question(cloneQuestion)
                                                .build())
                                            .collect(Collectors.toList());

            cloneQuestion.setAnswers(cloneAnswers);
            Question saved = questionRepository.save(cloneQuestion);
            return questionMapper.toResponseDTO(saved);

        }
        else{
            //Câu hỏi chưa nằm trong quiz nào
            oldQuestion.setText(request.getText());
            oldQuestion.setType(request.getType());
            oldQuestion.setCategory(category);

            if (request.getIsActive() != null) oldQuestion.setIsActive(request.getIsActive());
            if (request.getQuestionStatus() != null) oldQuestion.setQuestionStatus(request.getQuestionStatus());

            List<Answer> newAnswers = request.getAnswers().stream()
                    .map(ansDto -> Answer.builder()
                            .text(ansDto.getText())
                            .isCorrect(ansDto.getIsCorrect())
                            .question(oldQuestion)
                            .build())
                    .collect(Collectors.toList());

            oldQuestion.getAnswers().clear();
            oldQuestion.getAnswers().addAll(newAnswers);

            Question saved = questionRepository.save(oldQuestion);
            return questionMapper.toResponseDTO(saved);
        }
    }

    @Override
    public Page<QuestionResponseDTO> getQuestionsByTeacher(Long userId,Long categoryId, QuestionType type,
                                                    String keyword, int page, int size,
                                                    String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                            ? Sort.by(sortBy).ascending()
                            : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Question> questionPage = questionRepository.searchQuestionsByTeacher(categoryId, type, keyword, userId, pageable);

        return questionPage.map(questionMapper::toResponseDTO);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long userId, Long id) {
        Question question = questionRepository.findById(id)
                            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        if(!question.getCreator().getId().equals(userId)){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        question.setIsActive(false);
        questionRepository.save(question);
    }

    // Business Logic
    private void validateQuestionLogic(QuestionType type, List<AnswerCreationRequestDTO> answers) {
        if (answers.size() < 2) {
            throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC);
        }
        //Số lượng isCorrect = true
        long correctAnswersCount = answers.stream()
                .filter(AnswerCreationRequestDTO::getIsCorrect)
                .count();

        // Ràng buộc theo từng loại câu hỏi
        switch (type) {
            case SINGLE_CHOICE:
                if (correctAnswersCount != 1) {
                    throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC);
                }
                break;
            case MULTIPLE_CHOICE:
                if (correctAnswersCount < 2) {
                    throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC);
                }
                break;
            case FILL_IN_BLANK:
                if (correctAnswersCount < 1) {
                    throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC);
                }
                break;
        }
    }

    @Override
    @Transactional
    public void requestShareQuestion(Long questionId, Long teacherId) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!question.getCreator().getId().equals(teacherId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        // Chuyển trạng thái sang PENDING (chưa public ngay)
        question.setQuestionStatus(QuestionStatus.PENDING);
        questionRepository.save(question);
    }
    @Override
    @Transactional
    public void approveQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        question.setQuestionStatus(QuestionStatus.PUBLIC);
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public void rejectQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        question.setQuestionStatus(QuestionStatus.PRIVATE);
        questionRepository.save(question);
    }
}
