package com.example.quizhub.dto.category;

import java.util.ArrayList;
import java.util.List;

import com.example.quizhub.entity.Category;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponseDTO {
    Long id;
    String name;
    String description;
    Long parentId;
    List<CategoryResponseDTO> children;
    Boolean isPublic;
    Boolean isOwner;
    long quizCount;   // số quiz trong danh mục (public hoặc personal tuỳ context)

    // Constructor từ entity — dùng trong getAllCategories (xây cây thủ công)
    public CategoryResponseDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
        this.isPublic = category.getIsPublic();
        this.parentId = (category.getParent() != null) ? category.getParent().getId() : null;
        this.isOwner = false; // được set bởi service nếu cần
        this.children = new ArrayList<>();
        this.quizCount = 0;
    }
}
