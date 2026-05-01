package com.example.quizhub.repository;

import com.example.quizhub.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuizRepository extends JpaRepository<Quiz, UUID> {

    List<Quiz> findByCreatorId(Long creatorId);

    List<Quiz> findByIsDraftFalseAndIsEnableTrue();

    List<Quiz> findByIsExamTrue();

    /** Quiz công khai (published) theo danh mục */
    List<Quiz> findByCategoryIdAndIsDraftFalseAndIsEnableTrue(Long categoryId);

    /** Quiz của một người dùng cụ thể theo danh mục (bao gồm cả draft) */
    List<Quiz> findByCategoryIdAndCreatorId(Long categoryId, Long creatorId);

    /** Đếm quiz công khai theo danh mục (để hiển thị badge count) */
    long countByCategoryIdAndIsDraftFalseAndIsEnableTrue(Long categoryId);

    /** Đếm quiz cá nhân theo danh mục */
    long countByCategoryIdAndCreatorId(Long categoryId, Long creatorId);

    long countByCategoryId(Long categoryId);
}
