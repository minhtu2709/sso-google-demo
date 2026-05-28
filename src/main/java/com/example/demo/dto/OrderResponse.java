package com.example.demo.dto;

import com.example.demo.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Data
@Builder
public class OrderResponse {

    private Long id;
    private String status;
    private BigDecimal totalPrice;
    private String shippingAddress;
    private String recipientName;
    private String recipientPhone;
    private String paymentMethod;
    private String paymentStatus;
    private String paymentUrl; // Dành cho thanh toán online
    private String cancelReason;
    private List<OrderItemResponse> items;
    private String customerName;
    private LocalDateTime createdAt;

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderItemResponse::from)
                .toList();

        OrderResponse response = OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus().name())
                .totalPrice(order.getTotalPrice())
                .shippingAddress(order.getShippingAddress())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .cancelReason(order.getCancelReason())
                .items(itemResponses)
                .customerName(order.getUser().getName() != null ? order.getUser().getName() : order.getUser().getEmail())
                .createdAt(order.getCreatedAt())
                .build();

        // Nếu là đơn hàng chờ thanh toán online, tạo paymentUrl để khách có thể thanh toán lại
        // Cho phép thanh toán lại khi đơn hàng ở trạng thái PENDING hoặc CONFIRMED
        if ("UNPAID".equals(order.getPaymentStatus()) &&
            (order.getStatus() == Order.OrderStatus.PENDING || order.getStatus() == Order.OrderStatus.CONFIRMED) &&
            !"COD".equals(order.getPaymentMethod())) {
            
            long timestamp = order.getCreatedAt() != null 
                ? order.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                : System.currentTimeMillis();

            response.setPaymentUrl("/api/payment/simulate?orderId=" + order.getId() + 
                                   "&method=" + order.getPaymentMethod() + 
                                   "&createdAt=" + timestamp);
        }

        return response;
    }
}