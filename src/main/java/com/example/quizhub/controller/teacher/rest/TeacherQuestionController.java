// src/main/java/com/example/quizhub/controller/teacher/rest/TeacherQuestionController.java
package com.example.quizhub.controller.teacher.rest;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.example.quizhub.dto.question.QuestionRequestDTO;
import com.example.quizhub.dto.question.QuestionResponseDTO;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.QuestionType;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.question.QuestionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/teacher/questions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherQuestionController {

    private final QuestionService questionService;
    private final UserRepository userRepository;

    private Long getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return user.getId();
    }

    @PostMapping
    public ResponseEntity<QuestionResponseDTO> createQuestion(@RequestBody @Valid QuestionRequestDTO request) {
        QuestionResponseDTO response = questionService.createNewQuestion(getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ĐÃ SỬA: Thêm isPublicTab và rẽ nhánh gọi Service
    @GetMapping
    public ResponseEntity<Page<QuestionResponseDTO>> getQuestionsByTeacher(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "false") Boolean isPublicTab,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<QuestionResponseDTO> response;

        if (isPublicTab) {
            // Lấy kho hệ thống
            response = questionService.searchPublicQuestion(categoryId, type, keyword, page, size, sortBy, sortDir);
        } else {
            // Lấy kho cá nhân
            response = questionService.searchMyQuestion(getCurrentUserId(), categoryId, type, keyword, page, size, sortBy, sortDir);
        }

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponseDTO> updateQuestion(
            @PathVariable Long id,
            @RequestBody @Valid QuestionRequestDTO request) {
        return ResponseEntity.ok(questionService.updateQuestion(getCurrentUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(getCurrentUserId(), id);
        return ResponseEntity.noContent().build();
    }

    // MỚI THÊM: API xin chia sẻ (Xin Public)
    @PutMapping("/{id}/share")
    public ResponseEntity<String> requestShareQuestion(@PathVariable Long id) {
        questionService.requestShareQuestion(id, getCurrentUserId());
        return ResponseEntity.ok("Question has been submitted for approval");
    }
}
