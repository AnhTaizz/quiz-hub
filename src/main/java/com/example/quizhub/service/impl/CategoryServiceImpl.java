package com.example.quizhub.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.quizhub.dto.category.CategoryRequestDTO;
import com.example.quizhub.dto.category.CategoryResponseDTO;
import com.example.quizhub.entity.Category;
import com.example.quizhub.entity.Question;
import com.example.quizhub.entity.User;
import com.example.quizhub.entity.enums.Role;
import com.example.quizhub.exception.AppException;
import com.example.quizhub.exception.ErrorCode;
import com.example.quizhub.mapper.CategoryMapper;
import com.example.quizhub.repository.CategoryRepository;
import com.example.quizhub.repository.QuestionRepository;
import com.example.quizhub.repository.QuizRepository;
import com.example.quizhub.repository.UserRepository;
import com.example.quizhub.service.CategoryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final QuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final QuizRepository quizRepository;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public List<CategoryResponseDTO> getAllCategories(){
        List<Category> allCategories = categoryRepository.findAll();
        return buildTree(allCategories, null);
    }

    @Override
    public List<CategoryResponseDTO> getPublicCategories() {
        // JOIN FETCH đảm bảo parent được load trong transaction
        List<Category> publicCats = categoryRepository.findAllPublicWithParent();
        List<CategoryResponseDTO> dtos = buildTree(publicCats, null);
        attachPublicQuizCount(dtos);
        return dtos;
    }

    @Override
    public List<CategoryResponseDTO> getMyCategories() {
        User me = getCurrentUser();
        // JOIN FETCH đảm bảo parent được load trong transaction
        List<Category> myCats = categoryRepository.findAllByCreatorIdWithParent(me.getId());
        List<CategoryResponseDTO> dtos = buildTree(myCats, null);
        attachMyQuizCount(dtos, me.getId());
        return dtos;
    }

    // ─── Helper: Xây cây từ danh sách flat ───
    private List<CategoryResponseDTO> buildTree(List<Category> categories, Long parentId) {
        Map<Long, CategoryResponseDTO> dtoMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, CategoryResponseDTO::new));

        List<CategoryResponseDTO> roots = new ArrayList<>();
        for (Category c : categories) {
            CategoryResponseDTO dto = dtoMap.get(c.getId());
            if (c.getParent() == null) {
                roots.add(dto);
            } else {
                CategoryResponseDTO parentDto = dtoMap.get(c.getParent().getId());
                if (parentDto != null) parentDto.getChildren().add(dto);
                else roots.add(dto); // parent không trong list → treat as root
            }
        }
        return roots;
    }

    private void attachPublicQuizCount(List<CategoryResponseDTO> dtos) {
        for (CategoryResponseDTO dto : dtos) {
            List<Long> allIds = getAllDescendantIds(dto.getId());
            long count = quizRepository.countByCategoryIdInAndIsDraftFalseAndIsEnableTrue(allIds);
            dto.setQuizCount(count);
            if (dto.getChildren() != null) attachPublicQuizCount(dto.getChildren());
        }
    }

    private void attachMyQuizCount(List<CategoryResponseDTO> dtos, Long creatorId) {
        for (CategoryResponseDTO dto : dtos) {
            List<Long> allIds = getAllDescendantIds(dto.getId());
            long count = quizRepository.countByCategoryIdInAndCreatorId(allIds, creatorId);
            dto.setQuizCount(count);
            if (dto.getChildren() != null) attachMyQuizCount(dto.getChildren(), creatorId);
        }
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
        category.setCreator(getCurrentUser());
        // Kế thừa isPublic từ request; mặc định false nếu null
        if (request.getIsPublic() != null) category.setIsPublic(request.getIsPublic());
        else category.setIsPublic(false);

        return categoryMapper.toResponseDTO(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponseDTO updateCategory(Long id, CategoryRequestDTO request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isPublic = category.getIsPublic() != null && category.getIsPublic();
        boolean isCreator = category.getCreator() != null && category.getCreator().getId().equals(currentUser.getId());

        if (isPublic) {
            if (!isAdmin) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        } else {
            if (!isCreator && !isAdmin) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

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
        if (request.getIsPublic() != null) category.setIsPublic(request.getIsPublic());

        return categoryMapper.toResponseDTO(categoryRepository.save(category));
    }

//Xóa an toàn khi có danh mục con
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        boolean isPublic = category.getIsPublic() != null && category.getIsPublic();
        boolean isCreator = category.getCreator() != null && category.getCreator().getId().equals(currentUser.getId());

        if (isPublic) {
            if (!isAdmin) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        } else {
            if (!isCreator && !isAdmin) {
                throw new AppException(ErrorCode.UNAUTHORIZED);
            }
        }

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

    @Override
    public List<Long> getAllDescendantIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        collectIds(category, ids);
        return ids;
    }

    private void collectIds(Category category, List<Long> ids) {
        ids.add(category.getId());
        if (category.getChildren() != null) {
            for (Category child : category.getChildren()) {
                collectIds(child, ids);
            }
        }
    }
}
