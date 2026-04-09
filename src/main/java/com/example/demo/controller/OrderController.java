package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.service.OrderService;
import com.example.demo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    // POST /orders — đặt hàng từ giỏ hàng
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody OrderRequest request) {

        Long userId = getUserId(principal);
        OrderResponse response = orderService.placeOrder(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", response));
    }

    // GET /orders — lấy tất cả đơn hàng của mình
    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal Object principal) {

        Long userId = getUserId(principal);
        List<OrderResponse> response = orderService.getMyOrders(userId);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    // GET /orders/{id} — xem chi tiết 1 đơn hàng
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {

        Long userId = getUserId(principal);
        OrderResponse response = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    // PUT /orders/{id}/cancel — hủy đơn hàng
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {

        Long userId = getUserId(principal);
        OrderResponse response = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", response));
    }

    // Helper — lấy userId từ principal (giống CartController)
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