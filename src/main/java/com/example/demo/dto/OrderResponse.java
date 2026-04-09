package com.example.demo.dto;

import com.example.demo.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String status;
    private BigDecimal totalPrice;
    private String shippingAddress;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .shippingAddress(order.getShippingAddress())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}