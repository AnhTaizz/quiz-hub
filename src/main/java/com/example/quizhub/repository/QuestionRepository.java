package com.example.quizhub.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.entity.enums.QuestionType;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByCreatorId(Long creatorId);

    List<Question> findByCategoryId(Long categoryId);

    List<Question> findByType(QuestionType type);

    List<Question> findByQuestionStatus(QuestionStatus status);

    //Lấy câu hỏi theo category, type, keyword, hoặc public của user khác
    @Query("SELECT q FROM Question q WHERE q.isActive = true " +
           "AND (q.creator.id = :userId OR q.questionStatus = com.example.quizhub.entity.enums.QuestionStatus.PUBLIC)" +
           "AND (:categoryId IS NULL OR q.category.id = :categoryId) " +
           "AND (:type IS NULL OR q.type = :type) " +
           "AND (:keyword IS NULL OR LOWER(q.text) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> searchQuestionsByTeacher(@Param("categoryId") Long categoryId,
                                          @Param("type") QuestionType type,
                                          @Param("keyword") String keyword,
                                          @Param("userId") Long userId,
                                          Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END "
            + "FROM Quiz q JOIN q.questions quest "
            + "WHERE quest.id = :questionId")
    boolean isQuestionUsedInQuiz(@Param("questionId") Long questionId);

    // Tìm các câu hỏi đang chờ duyệt để Admin xử lý
    Page<Question> findByQuestionStatus(QuestionStatus status, Pageable pageable);
}
