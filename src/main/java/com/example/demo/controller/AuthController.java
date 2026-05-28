package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.ProfileUpdateRequest;
import com.example.demo.dto.ChangePasswordRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.service.UserService;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.entity.RefreshToken;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final SecurityUtils securityUtils;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đăng ký thành công", response));
    }

    // Login trả về JWT token
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {

        UserResponse user = userService.login(request);

        // Tạo JWT token từ email và role
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole());
        
        // Tạo Refresh Token
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        // Trả về cả token và thông tin user
        Map<String, Object> data = Map.of(
                "token", token,
                "refreshToken", refreshToken.getToken(),
                "user", user
        );

        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", data));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@RequestBody Map<String, String> request) {
        String requestRefreshToken = request.get("refreshToken");

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
                    return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", Map.of(
                            "token", token,
                            "refreshToken", requestRefreshToken
                    )));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal Object principal) {

        String email = securityUtils.extractEmail(principal);
        UserResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody ProfileUpdateRequest request) {

        String email = securityUtils.extractEmail(principal);
        UserResponse response = userService.updateProfile(email, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", response));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal Object principal,
            @RequestParam("file") MultipartFile file) {

        String email = securityUtils.extractEmail(principal);
        String avatarUrl = userService.updateAvatar(email, file);
        return ResponseEntity.ok(ApiResponse.success("Tải ảnh lên thành công", avatarUrl));
    }

    @PutMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody ChangePasswordRequest request) {

        String email = securityUtils.extractEmail(principal);
        userService.changePassword(email, request);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công"));
    }
}