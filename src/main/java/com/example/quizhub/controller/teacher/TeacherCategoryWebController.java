package com.example.quizhub.controller.teacher;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.quizhub.dto.category.CategoryRequestDTO;
import com.example.quizhub.service.category.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher/categories")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('TEACHER')")
public class TeacherCategoryWebController {

    private final CategoryService categoryService;

    @GetMapping("/public")
    public String getPublicCategoryPage(Model model) {
        model.addAttribute("categoryType", "public");
        return "teacher/teacher-category-management";
    }

    @GetMapping("/mine")
    public String getMineCategoryPage(Model model) {
        model.addAttribute("categoryType", "mine");
        return "teacher/teacher-category-management";
    }

    /** Render trang — dữ liệu được tải qua AJAX từ REST API */
    @GetMapping
    public String getCategoryPage(Model model) {
        model.addAttribute("categoryType", "mine");
        return "teacher/teacher-category-management";
    }

    @PostMapping("/save")
    public String saveCategory(@Valid CategoryRequestDTO request) {
        categoryService.createCategory(request);
        return "redirect:/teacher/categories";
    }

    @PostMapping("/update")
    public String updateCategory(@RequestParam Long id, @Valid CategoryRequestDTO request) {
        categoryService.updateCategory(id, request);
        return "redirect:/teacher/categories";
    }

    @PostMapping("/delete")
    public String deleteCategory(@RequestParam Long id) {
        categoryService.deleteCategory(id);
        return "redirect:/teacher/categories";
    }
}
