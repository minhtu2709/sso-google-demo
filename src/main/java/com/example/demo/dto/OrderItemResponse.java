package com.example.demo.dto;

import com.example.demo.entity.OrderItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Integer quantity;

    // Giá tại thời điểm mua — không phải giá hiện tại
    private BigDecimal priceAtTime;

    // Thành tiền
    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImageUrl(item.getProduct().getImageUrl())
                .quantity(item.getQuantity())
                .priceAtTime(item.getPriceAtTime())
                .subtotal(item.getPriceAtTime()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}