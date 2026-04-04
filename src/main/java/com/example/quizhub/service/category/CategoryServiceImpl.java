package com.example.quizhub.service.category;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.category.CategoryRequestDTO;
import com.example.quizhub.dto.category.CategoryResponseDTO;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Question;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.mapper.CategoryMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final QuestionRepository questionRepository;

    @Override
    public List<CategoryResponseDTO> getAllCategories(){
        List<Category> allCategories = categoryRepository.findAll();

        //Map id với CategoryResponseDTO
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
    public CategoryResponseDTO getCategoryById(Long id) {
        return categoryMapper.toResponseDTO(categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND)));
    }

    @Override
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        Category parent = null;
        if (request.getParentId() != null) {
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Category category = categoryMapper.toEntity(request);
        category.setParent(parent);

        return categoryMapper.toResponseDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        Category parent = null;
        if (request.getParentId() != null) {
            if (id.equals(request.getParentId())) {
                throw new AppException(ErrorCode.CATEGORY_INVALID_LOGIC);
            }
            parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Category tmp = parent;
        while(tmp != null){
            if(tmp.getId().equals(id)){
                throw new AppException(ErrorCode.CATEGORY_INVALID_LOGIC);
            }
            tmp = tmp.getParent();
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setParent(parent);

        return categoryMapper.toResponseDTO(categoryRepository.save(category));
    }

//Xóa an toàn khi có danh mục con
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        List<Question> questions = questionRepository.findByCategoryId(id);

        if (!questions.isEmpty()) {
            throw new AppException(ErrorCode.CATEGORY_HAS_QUESTIONS);
        }

        List<Category> children = category.getChildren();
        if(children != null && !children.isEmpty()){
            for(Category child : children){
                child.setParent(null);
                categoryRepository.save(child);
            }
        }
        categoryRepository.delete(category);
    }
}
