package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final com.example.demo.service.UserService userService;

    // GET /cart — xem giỏ hàng
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal Object principal) {

        Long userId = getUserId(principal);
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    // POST /cart/items — thêm sản phẩm vào giỏ
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody CartItemRequest request) {

        Long userId = getUserId(principal);
        CartResponse response = cartService.addItem(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ thành công", response));
    }

    // DELETE /cart/items/{cartItemId} — xóa 1 sản phẩm khỏi giỏ
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long cartItemId) {

        Long userId = getUserId(principal);
        CartResponse response = cartService.removeItem(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ", response));
    }

    // DELETE /cart — xóa toàn bộ giỏ hàng
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal Object principal) {

        Long userId = getUserId(principal);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa giỏ hàng"));
    }

    // Helper — lấy userId từ principal
    // Principal là object đại diện cho người đang đăng nhập
    // Có thể là OAuth2User (Google) hoặc UserDetails (local)
    private Long getUserId(Object principal) {
        String email;

        if (principal instanceof OAuth2User oAuth2User) {
            email = oAuth2User.getAttribute("email");
        } else if (principal instanceof UserDetails userDetails) {
            email = userDetails.getUsername();
        } else {
            throw new IllegalArgumentException("Chưa đăng nhập");
        }

        return userService.getUserByEmail(email).getId();
    }
}