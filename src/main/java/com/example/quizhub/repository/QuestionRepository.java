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

    // 1. Kho Riêng: Lấy câu hỏi của cá nhân (Truyền List chứa PRIVATE và PENDING vào để tránh lỗi Enum)
    @Query("SELECT q FROM Question q " +
           "WHERE q.creator.id = :userId " +
           "AND q.questionStatus IN :statuses " +
           "AND (:categoryId IS NULL OR q.category.id = :categoryId) " +
           "AND (:type IS NULL OR q.type = :type) " +
           "AND (:keyword IS NULL OR LOWER(q.text) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> searchMyQuestion(@Param("userId") Long userId,
                                     @Param("categoryId") Long categoryId,
                                     @Param("type") QuestionType type,
                                     @Param("keyword") String keyword,
                                     @Param("statuses") List<QuestionStatus> statuses,
                                     Pageable pageable);

    // 2. Kho Chung: Lấy câu hỏi đã được Public của hệ thống
    @Query("SELECT q FROM Question q " +
           "WHERE q.questionStatus = :status " +
           "AND (:categoryId IS NULL OR q.category.id = :categoryId) " +
           "AND (:type IS NULL OR q.type = :type) " +
           "AND (:keyword IS NULL OR LOWER(q.text) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Question> searchPublicQuestion(@Param("categoryId") Long categoryId,
                                         @Param("type") QuestionType type,
                                         @Param("keyword") String keyword,
                                         @Param("status") QuestionStatus status,
                                         Pageable pageable);

    // Kiểm tra câu hỏi đã nằm trong đề thi nào chưa
    @Query("SELECT CASE WHEN COUNT(q) > 0 THEN true ELSE false END "
            + "FROM Quiz q JOIN q.questions quest "
            + "WHERE quest.id = :questionId")
    boolean isQuestionUsedInQuiz(@Param("questionId") Long questionId);

    // Tìm các câu hỏi đang chờ duyệt để Admin xử lý
    Page<Question> findByQuestionStatus(QuestionStatus status, Pageable pageable);

    long countByQuestionStatus(QuestionStatus status);

    long countByCategoryId(Long categoryId);

    java.util.List<Question> findTop5ByQuestionStatusOrderByIdDesc(QuestionStatus status);
}
