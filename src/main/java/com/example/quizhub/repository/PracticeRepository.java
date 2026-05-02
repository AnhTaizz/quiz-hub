package com.example.quizhub.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.quizhub.entity.Practice;

@Repository
public interface PracticeRepository extends JpaRepository<Practice, Long> {
    List<Practice> findByUserIdAndCategoryIdOrderByCreatedAtDesc(Long userId, Long categoryId);
}

