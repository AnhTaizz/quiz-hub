package com.example.quizhub.repository;

import com.example.quizhub.entity.AttemptViolation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttemptViolationRepository extends JpaRepository<AttemptViolation, UUID> {

    List<AttemptViolation> findByAttemptId(Long attemptId);
}
