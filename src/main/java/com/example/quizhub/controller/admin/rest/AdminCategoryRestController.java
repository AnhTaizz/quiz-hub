package com.example.quizhub.controller.admin.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.quizhub.dto.category.CategoryRequestDTO;
import com.example.quizhub.dto.category.CategoryResponseDTO;
import com.example.quizhub.dto.quiz.QuizSummaryDTO;
import com.example.quizhub.service.category.CategoryService;
import com.example.quizhub.service.quiz.QuizService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryRestController {

    private final CategoryService categoryService;
    private final QuizService quizService;
    private final com.example.quizhub.repository.QuestionRepository questionRepository;
    private final com.example.quizhub.mapper.QuestionMapper questionMapper;

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getPublicCategories() {
        // Trả về toàn bộ danh mục public cho admin quản lý
        return ResponseEntity.ok(categoryService.getPublicCategories());
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> createCategory(@RequestBody @Valid CategoryRequestDTO request) {
        request.setIsPublic(true); // Ép kiểu luôn tạo danh mục công khai
        CategoryResponseDTO response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/{id}/quizzes")
    public ResponseEntity<List<QuizSummaryDTO>> getPublicQuizzes(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getPublicQuizzesByCategoryId(id));
    }

    @GetMapping("/{id}/questions")
    public ResponseEntity<org.springframework.data.domain.Page<com.example.quizhub.dto.question.QuestionResponseDTO>> getQuestionsByCategoryId(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.by("id").descending());
        org.springframework.data.domain.Page<com.example.quizhub.entity.Question> questionPage = questionRepository.findByCategoryId(id, pageable);
        return ResponseEntity.ok(questionPage.map(questionMapper::toResponseDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid CategoryRequestDTO request) {
        request.setIsPublic(true); // Luôn là danh mục công khai
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
