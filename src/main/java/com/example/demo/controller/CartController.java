package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.service.CartService;
import com.example.demo.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal Object principal) {

        Long userId = securityUtils.getCurrentUserId(principal);
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody CartItemRequest request) {

        Long userId = securityUtils.getCurrentUserId(principal);
        CartResponse response = cartService.addItem(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Thêm vào giỏ thành công", response));
    }

    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long cartItemId) {

        Long userId = securityUtils.getCurrentUserId(principal);
        CartResponse response = cartService.removeItem(userId, cartItemId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa sản phẩm khỏi giỏ", response));
    }

    @PutMapping("/items/{cartItemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {

        Long userId = securityUtils.getCurrentUserId(principal);
        CartResponse response = cartService.updateItemQuantity(userId, cartItemId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật số lượng thành công", response));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal Object principal) {

        Long userId = securityUtils.getCurrentUserId(principal);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã xóa giỏ hàng"));
    }
}
