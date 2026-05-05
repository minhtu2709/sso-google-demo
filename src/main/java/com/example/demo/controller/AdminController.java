package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.RoleUpdateRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách người dùng thành công", users));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody com.example.demo.dto.ProfileUpdateRequest request) {
        UserResponse response = userService.adminUpdateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thông tin người dùng thành công", response));
    }

    @PutMapping("/users/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changeUserPassword(
            @PathVariable Long id,
            @RequestBody Map<String, String> request) {
        String newPassword = request.get("newPassword");
        userService.adminChangeUserPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật mật khẩu người dùng thành công"));
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @Valid @RequestBody RoleUpdateRequest request) {

        UserResponse response = userService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật role thành công", response));
    }
}