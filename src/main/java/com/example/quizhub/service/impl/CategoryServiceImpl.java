package com.example.quizhub.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.category.CategoryRequestDTO;
import com.example.quizhub.dto.category.CategoryResponseDTO;
import com.example.quizhub.entity.Category;
import com.example.quizhub.mapper.CategoryMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.service.CategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponseDTO> getAllCategories(){
        List<Category> allCategories = categoryRepository.findAll();

        Map<Long, CategoryResponseDTO> dtoMap = allCategories.stream()
                .collect(Collectors.toMap(Category::getId, CategoryResponseDTO::new));

        List<CategoryResponseDTO> rootCategories = new ArrayList<>();

        for(Category category : allCategories){
            CategoryResponseDTO currentDto = dtoMap.get(category.getId());

            if (category.getParent() == null) {
                // Không có cha → là root
                rootCategories.add(currentDto);
            }
            else{
                CategoryResponseDTO parentDto = dtoMap.get(category.getParent().getId());
                if(parentDto != null){
                    parentDto.getChildren().add(currentDto);
                }
            }
        }
        return rootCategories;
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục cha với id: " + request.getParentId()));
        }

        Category category = categoryMapper.toCategory(request);
        category.setParent(parent);

        return categoryMapper.toCategoryResponseDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));

        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục cha với id: " + request.getParentId()));
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setParent(parent);

        return categoryMapper.toCategoryResponseDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy danh mục với id: " + id));
        categoryRepository.delete(category);
    }
}
