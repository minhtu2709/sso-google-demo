package com.example.demo.dto;

import com.example.demo.entity.Cart;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartResponse {

    private Long id;
    private List<CartItemResponse> items;

    // Tổng tiền toàn bộ giỏ hàng
    private BigDecimal totalPrice;

    // Tổng số sản phẩm trong giỏ
    private Integer totalItems;

    public static CartResponse from(Cart cart) {
        List<CartItemResponse> itemResponses = cart.getItems().stream()
                .map(CartItemResponse::from)
                .toList();

        // Tính tổng tiền bằng cách cộng subtotal của từng item
        BigDecimal totalPrice = itemResponses.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .items(itemResponses)
                .totalPrice(totalPrice)
                .totalItems(itemResponses.size())
                .build();
    }
}