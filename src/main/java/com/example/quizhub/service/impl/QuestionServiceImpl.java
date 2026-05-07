package com.example.quizhub.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.question.AnswerCreationRequestDTO;
import com.example.quizhub.dto.question.QuestionRequestDTO;
import com.example.quizhub.dto.question.QuestionResponseDTO;
import com.example.quizhub.entity.Answer;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.QuestionLevel;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.entity.enums.QuestionType;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.mapper.QuestionMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.QuestionService;
import com.example.quizhub.repository.AnswerRepository;
import com.example.quizhub.entity.enums.QuestionLevel;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.entity.enums.QuestionType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final CategoryRepository categoryRepository;
    private final AnswerRepository answerRepository;
    private final QuestionMapper questionMapper;

    // Business Logic
    private void validateQuestionLogic(QuestionType type, List<AnswerCreationRequestDTO> answers) {
        if (answers == null || answers.isEmpty()) {
            throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC); // Phải có ít nhất 1 đáp án
        }

        // Khắc phục lỗi NullPointerException bằng Boolean.TRUE.equals()
        long correctAnswersCount = answers.stream()
                .filter(ans -> Boolean.TRUE.equals(ans.getCorrect()))
                .count();

        // Ràng buộc linh hoạt theo từng loại câu hỏi
        switch (type) {
            case SINGLE_CHOICE:
                // Trắc nghiệm 1 đáp án: Bắt buộc >= 2 lựa chọn, và đúng 1 đáp án đúng
                if (answers.size() < 2 || correctAnswersCount != 1) {
                    throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC);
                }
                break;
            case MULTIPLE_CHOICE:
                // Trắc nghiệm nhiều đáp án: Bắt buộc >= 2 lựa chọn, và >= 2 đáp án đúng
                if (answers.size() < 2 || correctAnswersCount < 2) {
                    throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC);
                }
                break;
            case FILL_IN_BLANK:
                // Điền khuyết: Có thể chỉ có 1 lựa chọn, nhưng lựa chọn đó bắt buộc phải đánh dấu là "đúng"
                if (correctAnswersCount < 1) {
                    throw new AppException(ErrorCode.QUESTION_INVALID_LOGIC);
                }
                break;
        }
    }

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
                            .questionStatus(QuestionStatus.PRIVATE)
                            .creator(creator)
                            .category(category)
                            .level(request.getLevel() != null ? request.getLevel() : QuestionLevel.MEDIUM)
                            .build();

        List<Answer> answers = request.getAnswers().stream()
                                    .map(ansDto -> Answer.builder()
                                                    .text(ansDto.getText())
                                                    .isCorrect(Boolean.TRUE.equals(ansDto.getCorrect()))
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
        boolean isUsedInPractice = questionRepository.isQuestionUsedInPractice(id);

        if(isUsedInQuiz || isUsedInPractice){
            //xóa câu hỏi cũ nếu đã dùng trong quiz hoặc luyện tập rồi
            oldQuestion.setQuestionStatus(QuestionStatus.DELETED);
            questionRepository.save(oldQuestion);
            //tạo câu hỏi mới
            Question cloneQuestion = Question.builder()
                                        .type(request.getType())
                                        .text(request.getText())
                                        .creator(oldQuestion.getCreator())
                                        .category(category)
                                        .questionStatus(QuestionStatus.PRIVATE)
                                        .level(request.getLevel() != null ? request.getLevel() : QuestionLevel.MEDIUM)
                                        .build();

            List<Answer> cloneAnswers = request.getAnswers().stream()
                                        .map(ansDto -> Answer.builder()
                                                .text(ansDto.getText())
                                                .isCorrect(Boolean.TRUE.equals(ansDto.getCorrect()))
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
            oldQuestion.setQuestionStatus(QuestionStatus.PRIVATE);
            if (request.getLevel() != null) {
                oldQuestion.setLevel(request.getLevel());
            }

            List<Answer> newAnswers = request.getAnswers().stream()
                    .map(ansDto -> Answer.builder()
                            .text(ansDto.getText())
                            .isCorrect(Boolean.TRUE.equals(ansDto.getCorrect()))
                            .question(oldQuestion)
                            .build())
                    .collect(Collectors.toList());

            oldQuestion.getAnswers().clear();
            oldQuestion.getAnswers().addAll(newAnswers);

            Question saved = questionRepository.save(oldQuestion);
            return questionMapper.toResponseDTO(saved);
        }
    }

    private void collectCategoryIds(Category category, List<Long> ids) {
        if (category == null) return;
        ids.add(category.getId());
        if (category.getChildren() != null) {
            for (Category child : category.getChildren()) {
                collectCategoryIds(child, ids);
            }
        }
    }

    @Override
    public Page<QuestionResponseDTO> searchMyQuestion(Long userId, Long categoryId, QuestionType type,
                                                    String keyword,
                                                    int page, int size,
                                                    String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                            ? Sort.by(sortBy).ascending()
                            : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        boolean useCategoryFilter = false;
        List<Long> categoryIds = new java.util.ArrayList<>();
        if (categoryId != null) {
            if (categoryId == -1L) {
                categoryIds.add(-1L);
            } else {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
                collectCategoryIds(category, categoryIds);
            }
            useCategoryFilter = true;
        }

        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : "%" + keyword.trim().toLowerCase() + "%";
        Page<Question> questionPage = questionRepository.searchMyQuestion(userId, useCategoryFilter, categoryIds, type, searchKeyword, pageable);

        return questionPage.map(questionMapper::toResponseDTO);
    }

    @Override
    public Page<QuestionResponseDTO> searchPublicQuestion(Long categoryId, QuestionType type,
                                                          String keyword, int page, int size,
                                                          String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        boolean useCategoryFilter = false;
        List<Long> categoryIds = new java.util.ArrayList<>();
        if (categoryId != null) {
            if (categoryId == -1L) {
                categoryIds.add(-1L);
            } else {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
                collectCategoryIds(category, categoryIds);
            }
            useCategoryFilter = true;
        }

        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : "%" + keyword.trim().toLowerCase() + "%";
        Page<Question> questionPage = questionRepository.searchPublicQuestion(useCategoryFilter, categoryIds, type, searchKeyword, QuestionStatus.PUBLIC, pageable);

        return questionPage.map(questionMapper::toResponseDTO);
    }

    @Override
    public Page<QuestionResponseDTO> searchQuestions(QuestionStatus status, Long categoryId, QuestionType type,
                                                     QuestionLevel level, String keyword, String creatorName,
                                                     int page, int size, String sortBy, String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        boolean useCategoryFilter = false;
        List<Long> categoryIds = new java.util.ArrayList<>();
        if (categoryId != null) {
            if (categoryId == -1L) {
                categoryIds.add(-1L);
            } else {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
                collectCategoryIds(category, categoryIds);
            }
            useCategoryFilter = true;
        }

        String searchKeyword = (keyword == null || keyword.trim().isEmpty()) ? null : "%" + keyword.trim().toLowerCase() + "%";
        String searchCreator = (creatorName == null || creatorName.trim().isEmpty()) ? null : "%" + creatorName.trim().toLowerCase() + "%";

        Page<Question> questionPage = questionRepository.searchQuestions(status, useCategoryFilter, categoryIds, type, level, searchKeyword, searchCreator, pageable);

        return questionPage.map(questionMapper::toResponseDTO);
    }

    @Override
    public QuestionResponseDTO getQuestionById(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        return questionMapper.toResponseDTO(question);
    }

    @Override
    @Transactional
    public void deleteQuestion(Long userId, Long id) {
        Question question = questionRepository.findById(id)
                            .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        if(!question.getCreator().getId().equals(userId)){
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        question.setQuestionStatus(QuestionStatus.DELETED);
        questionRepository.save(question);
    }


    @Override
    @Transactional
    public void requestShareQuestion(Long questionId, Long teacherId) {
        Question question = questionRepository.findById(questionId).orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        // Kiểm tra quyền sở hữu
        if (!question.getCreator().getId().equals(teacherId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if(question.getQuestionStatus() == QuestionStatus.PUBLIC){
            throw new AppException(ErrorCode.QUESTION_ALREADY_PUBLIC);
        }

        // Chuyển trạng thái sang PENDING (chưa public ngay)
        question.setQuestionStatus(QuestionStatus.PENDING);
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public void bulkRequestShareQuestions(List<Long> questionIds, Long teacherId) {
        if (questionIds == null || questionIds.isEmpty()) return;
        for (Long id : questionIds) {
            requestShareQuestion(id, teacherId);
        }
    }

    @Override
    @Transactional
    public void bulkRequestShareAllQuestions(Long teacherId, Long categoryId, QuestionType type, String keyword) {
        String searchKeyword = (keyword != null && !keyword.isEmpty()) ? "%" + keyword.toLowerCase() + "%" : null;
        List<Long> ids = questionRepository.findIdsByFilters(teacherId, QuestionStatus.PRIVATE, categoryId, type, searchKeyword);
        bulkRequestShareQuestions(ids, teacherId);
    }

    //Admin duyệt
    @Override
    @Transactional
    public void approveQuestion(Long questionId, Long categoryId) {
        Question originalQuestion = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        // Tạo câu hỏi mới (clone) cho mục PUBLIC
        Question clone = new Question();
        clone.setText(originalQuestion.getText());
        clone.setType(originalQuestion.getType());
        clone.setLevel(originalQuestion.getLevel());
        clone.setCreator(originalQuestion.getCreator());
        clone.setCategory(category);
        clone.setQuestionStatus(QuestionStatus.PUBLIC);

        // Lưu clone
        Question savedClone = questionRepository.save(clone);

        // Sao chép các đáp án
        if (originalQuestion.getAnswers() != null) {
            java.util.List<Answer> clonedAnswers = new java.util.ArrayList<>();
            for (Answer ans : originalQuestion.getAnswers()) {
                Answer clonedAns = new Answer();
                clonedAns.setText(ans.getText());
                clonedAns.setIsCorrect(ans.getIsCorrect());
                clonedAns.setQuestion(savedClone);
                clonedAnswers.add(clonedAns);
            }
            answerRepository.saveAll(clonedAnswers);
            savedClone.setAnswers(clonedAnswers);
        }

        // Chuyển trạng thái của câu hỏi gốc về PRIVATE
        originalQuestion.setQuestionStatus(QuestionStatus.PRIVATE);
        questionRepository.save(originalQuestion);
    }

    @Override
    @Transactional
    public void bulkApproveQuestions(List<Long> questionIds, Long categoryId) {
        if (questionIds == null || questionIds.isEmpty()) return;
        for (Long id : questionIds) {
            approveQuestion(id, categoryId);
        }
    }

    @Override
    @Transactional
    public void bulkApproveAllQuestions(Long targetCategoryId, Long filterCategoryId, QuestionType type, QuestionLevel level, String keyword, String creatorName) {
        boolean useCategoryFilter = false;
        List<Long> categoryIds = new java.util.ArrayList<>();
        if (filterCategoryId != null) {
            if (filterCategoryId == -1L) {
                categoryIds.add(-1L);
            } else {
                Category category = categoryRepository.findById(filterCategoryId)
                        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
                collectCategoryIds(category, categoryIds);
            }
            useCategoryFilter = true;
        }

        String searchKeyword = (keyword != null && !keyword.isEmpty()) ? "%" + keyword.toLowerCase() + "%" : null;
        String searchCreator = (creatorName != null && !creatorName.isEmpty()) ? "%" + creatorName.toLowerCase() + "%" : null;
        List<Long> ids = questionRepository.findPendingIdsByFilters(QuestionStatus.PENDING, useCategoryFilter, categoryIds, type, level, searchKeyword, searchCreator);
        bulkApproveQuestions(ids, targetCategoryId);
    }

    //Admin từ chối
    @Override
    @Transactional
    public void rejectQuestion(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        question.setQuestionStatus(QuestionStatus.PRIVATE);
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public void bulkRejectQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) return;
        for (Long id : questionIds) {
            rejectQuestion(id);
        }
    }

    @Override
    @Transactional
    public void bulkRejectAllQuestions(Long filterCategoryId, QuestionType type, QuestionLevel level, String keyword, String creatorName) {
        boolean useCategoryFilter = false;
        List<Long> categoryIds = new java.util.ArrayList<>();
        if (filterCategoryId != null) {
            if (filterCategoryId == -1L) {
                categoryIds.add(-1L);
            } else {
                Category category = categoryRepository.findById(filterCategoryId)
                        .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
                collectCategoryIds(category, categoryIds);
            }
            useCategoryFilter = true;
        }

        String searchKeyword = (keyword != null && !keyword.isEmpty()) ? "%" + keyword.toLowerCase() + "%" : null;
        String searchCreator = (creatorName != null && !creatorName.isEmpty()) ? "%" + creatorName.toLowerCase() + "%" : null;
        List<Long> ids = questionRepository.findPendingIdsByFilters(QuestionStatus.PENDING, useCategoryFilter, categoryIds, type, level, searchKeyword, searchCreator);
        bulkRejectQuestions(ids);
    }

    //Admin xóa
    @Override
    @Transactional
    public void deleteQuestionByAdmin(Long questionId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));

        question.setQuestionStatus(QuestionStatus.DELETED);
        questionRepository.save(question);
    }

    @Override
    @Transactional
    public void moveQuestionByAdmin(Long questionId, Long categoryId) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new AppException(ErrorCode.QUESTION_NOT_FOUND));
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        question.setCategory(category);
        questionRepository.save(question);
    }
}
