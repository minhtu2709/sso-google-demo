package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.service.OrderService;
import com.example.demo.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @AuthenticationPrincipal Object principal,
            @Valid @RequestBody OrderRequest request) {

        Long userId = securityUtils.getCurrentUserId(principal);
        OrderResponse response = orderService.placeOrder(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đặt hàng thành công", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal Object principal) {

        Long userId = securityUtils.getCurrentUserId(principal);
        List<OrderResponse> response = orderService.getMyOrders(userId);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {

        Long userId = securityUtils.getCurrentUserId(principal);
        OrderResponse response = orderService.getOrderById(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal Object principal,
            @PathVariable Long id) {

        Long userId = securityUtils.getCurrentUserId(principal);
        OrderResponse response = orderService.cancelOrder(id, userId);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", response));
    }
}