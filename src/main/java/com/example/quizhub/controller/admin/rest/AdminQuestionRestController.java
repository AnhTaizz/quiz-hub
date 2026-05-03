package com.example.quizhub.controller.admin.rest;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.quizhub.dto.question.QuestionResponseDTO;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.enums.QuestionStatus;
import com.example.quizhub.mapper.QuestionMapper;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.service.question.QuestionService;

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
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase(Sort.Direction.ASC.name())
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Question> questionPage = questionRepository.findByQuestionStatus(QuestionStatus.PENDING, pageable);
        return ResponseEntity.ok(questionPage.map(questionMapper::toResponseDTO));
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
}
