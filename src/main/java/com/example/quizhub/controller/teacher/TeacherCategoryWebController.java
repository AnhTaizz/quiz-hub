package com.example.quizhub.controller.teacher;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.quizhub.dto.category.CategoryRequestDTO;
import com.example.quizhub.dto.category.CategoryResponseDTO;
import com.example.quizhub.service.category.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/teacher/categories")
@RequiredArgsConstructor
//@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class TeacherCategoryWebController {

    private final CategoryService categoryService;

    // View: Hiển thị trang quản lý category đệ quy
    @GetMapping
    public String getCategoryPage(Model model) {
        List<CategoryResponseDTO> rootCategories = categoryService.getAllCategories();
        model.addAttribute("rootCategories", rootCategories);
        return "teacher-category";
    }

    // Action: Xử lý submit form (thêm danh mục cha/con)
    @PostMapping("/save")
    public String saveCategory(@Valid CategoryRequestDTO request) {
        // Form web truyền request trực tiếp (Content-Type: application/x-www-form-urlencoded)
        categoryService.createCategory(request);
        return "redirect:/teacher/categories";
    }
}
