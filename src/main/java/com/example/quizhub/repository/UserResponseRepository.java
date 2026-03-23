package com.example.quizhub.repository;

import com.example.quizhub.entity.UserResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserResponseRepository extends JpaRepository<UserResponse, Long> {

    List<UserResponse> findByAttemptId(Long attemptId);

    List<UserResponse> findByQuestionId(Long questionId);
}
