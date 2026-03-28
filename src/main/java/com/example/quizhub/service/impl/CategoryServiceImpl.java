package com.example.quizhub.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.quizhub.dto.CategoryDTO;
import com.example.quizhub.entity.Category;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.service.CategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Override
    public List<CategoryDTO> getAllCategories(){
        List<Category> allCategories = categoryRepository.findAll();

        Map<Integer, CategoryDTO> dtoMap = allCategories.stream()
                                            .collect(Collectors.toMap(
                                                Category::getId,
                                                CategoryDTO::new
                                            ));

        List<CategoryDTO> rootCategories = new ArrayList<>();

        for(Category category : allCategories){
            CategoryDTO currentDto = dtoMap.get(category.getId());

            if (category.getParent() == null) {
                // Không có cha → là root
                rootCategories.add(currentDto);
            }
            else{
                CategoryDTO parentDto = dtoMap.get(category.getParent().getId());
                if(parentDto != null){
                    parentDto.getChildren().add(currentDto);
                }
            }
        }
        return rootCategories;
    }
}
