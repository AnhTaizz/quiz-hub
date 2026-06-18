package com.example.quizhub.controller.admin.rest;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.example.quizhub.dto.question.QuestionRequestDTO;
import com.example.quizhub.dto.question.QuestionResponseDTO;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.enums.QuestionLevel;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.entity.enums.QuestionType;
import com.example.quizhub.mapper.QuestionMapper;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.service.QuestionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuestionRestController {

    private final QuestionService questionService;
    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    @GetMapping("/pending")
    public ResponseEntity<Page<QuestionResponseDTO>> getPendingQuestions(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) QuestionLevel level,
            @RequestParam(required = false) String creatorName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(questionService.searchQuestions(
                QuestionStatus.PENDING, categoryId, type, level, keyword, creatorName, page, size, sortBy, sortDir));
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveQuestion(
            @PathVariable Long id,
            @RequestParam(required = false) Long categoryId) {
        questionService.approveQuestion(id, categoryId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectQuestion(@PathVariable Long id) {
        questionService.rejectQuestion(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/bulk-approve")
    public ResponseEntity<Void> bulkApprove(
            @RequestBody List<Long> ids,
            @RequestParam(required = false) Long categoryId) {
        questionService.bulkApproveQuestions(ids, categoryId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/bulk-approve-all")
    public ResponseEntity<Void> bulkApproveAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long filterCategoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) QuestionLevel level,
            @RequestParam(required = false) String creatorName) {
        questionService.bulkApproveAllQuestions(categoryId, filterCategoryId, type, level, keyword, creatorName);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/bulk-reject")
    public ResponseEntity<Void> bulkReject(@RequestBody List<Long> ids) {
        questionService.bulkRejectQuestions(ids);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/bulk-reject-all")
    public ResponseEntity<Void> bulkRejectAll(
            @RequestParam(required = false) Long filterCategoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) QuestionLevel level,
            @RequestParam(required = false) String creatorName) {
        questionService.bulkRejectAllQuestions(filterCategoryId, type, level, keyword, creatorName);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestionByAdmin(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/move")
    public ResponseEntity<Void> moveQuestion(
            @PathVariable Long id,
            @RequestParam Long categoryId) {
        questionService.moveQuestionByAdmin(id, categoryId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponseDTO> getQuestionById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestionById(id));
    }

    @PutMapping("/{id}/edit")
    public ResponseEntity<QuestionResponseDTO> editPublicQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequestDTO request) {
        return ResponseEntity.ok(questionService.updateQuestionByAdmin(id, request));
    }
}
