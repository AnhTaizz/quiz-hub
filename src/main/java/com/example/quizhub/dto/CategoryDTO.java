package com.example.quizhub.dto;

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
public class CategoryDTO {
    Integer id;
    String name;
    String description;
    Integer parentId;
    List<CategoryDTO> children;

    // Lưu ý: không gọi getParent().getId() trực tiếp — phải check null trước.

    public CategoryDTO(Category category) {
        this.id = category.getId();
        this.name = category.getName();
        this.description = category.getDescription();
        // FIX: check parent null trước khi gọi getId()
        this.parentId = (category.getParent() != null) ? category.getParent().getId() : null;
        this.children = new ArrayList<>();
    }
}
