package com.example.quizhub.mapper;

import com.example.quizhub.dto.category.CategoryRequestDTO;
import com.example.quizhub.dto.category.CategoryResponseDTO;
import com.example.quizhub.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    // Category entity -> CategoryResponseDTO (map đơn lẻ, không xây cây children)
    @Mapping(source = "parent.id", target = "parentId")
    @Mapping(target = "children",  ignore = true)
    CategoryResponseDTO toCategoryResponseDTO(Category category);

    // CategoryRequestDTO -> Category (bỏ qua parent/children vì service tự set)
    @Mapping(target = "id",       ignore = true)
    @Mapping(target = "parent",   ignore = true)
    @Mapping(target = "children", ignore = true)
    Category toCategory(CategoryRequestDTO request);
}
