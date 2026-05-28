package com.example.demo.service;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    /**
     * Tự động hủy các đơn hàng PENDING quá 15 phút mà chưa thanh toán.
     * Chạy mỗi phút một lần.
     */
    @Scheduled(fixedRate = 60000) // 60,000 ms = 1 phút
    @Transactional
    public void cancelExpiredOrders() {
        LocalDateTime expirationTime = LocalDateTime.now().minusMinutes(15);
        
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                Order.OrderStatus.PENDING, 
                expirationTime
        );

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("Phát hiện {} đơn hàng quá hạn thanh toán. Bắt đầu xử lý hủy...", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // Chỉ hủy những đơn hàng chưa thanh toán (Tránh trường hợp hi hữu thanh toán xong nhưng status chưa kịp update)
                if (!"PAID".equals(order.getPaymentStatus())) {
                    log.info("Đang tự động hủy đơn hàng ID: {}", order.getId());
                    
                    // 1. Hoàn lại tồn kho
                    for (OrderItem item : order.getItems()) {
                        productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
                    }

                    // 2. Cập nhật trạng thái
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    order.setPaymentStatus("EXPIRED");
                    order.setCancelReason("Hết hạn thanh toán");
                    orderRepository.save(order);
                }
            } catch (Exception e) {
                log.error("Lỗi khi tự động hủy đơn hàng {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}
