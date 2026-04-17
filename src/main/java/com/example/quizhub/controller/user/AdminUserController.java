package com.example.quizhub.controller.user;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.quizhub.dto.user.response.UserProfileResponse;
import com.example.quizhub.dto.user.response.UserResponseDTO;
import com.example.quizhub.entity.enums.Role;
import com.example.quizhub.service.user.UserService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<String> changeUserStatus(@PathVariable Long id, @RequestParam boolean enable) {
        userService.changeUserStatus(id, enable);
        String message = enable ? "Đã MỞ KHÓA tài khoản thành công!" : "Đã KHÓA tài khoản thành công!";
        return ResponseEntity.ok(message);
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<String> changeUserRole(@PathVariable Long id, @RequestParam Role role) {
        userService.changeUserRole(id, role);
        String message = "Đã cập nhật quyền của User ID " + id + " thành " + role.name() + " thành công!";
        return ResponseEntity.ok(message);
    }

    @GetMapping
    public ResponseEntity<Page<UserProfileResponse>> getAllUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getAllUsers(keyword, role, page, size));
    }
}
