package com.example.demo.dto;

import com.example.demo.entity.CartItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String imageUrl;

    // Giá hiện tại của sản phẩm
    private BigDecimal price;
    private Integer quantity;

    // Thành tiền = giá x số lượng
    // Tính luôn ở đây cho frontend khỏi tính
    private BigDecimal subtotal;

    public static CartItemResponse from(CartItem item) {
        return CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .imageUrl(item.getProduct().getImageUrl())
                .price(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                // subtotal = giá x số lượng
                .subtotal(item.getProduct().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}