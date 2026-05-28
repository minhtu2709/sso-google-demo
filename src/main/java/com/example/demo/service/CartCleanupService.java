package com.example.demo.service;

import com.example.demo.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartCleanupService {

    private final CartItemRepository cartItemRepository;

    // Chạy mỗi giờ một lần để dọn dẹp database
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredCartItems() {
        // Hết hạn sau 24 giờ
        LocalDateTime expiryDate = LocalDateTime.now().minusHours(24);
        log.info("Bắt đầu dọn dẹp giỏ hàng, xóa các mục trước ngày: {}", expiryDate);
        cartItemRepository.deleteExpiredItems(expiryDate);
    }
}
