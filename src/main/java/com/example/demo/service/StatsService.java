package com.example.demo.service;

import com.example.demo.dto.DashboardStatsResponse;
import com.example.demo.entity.Order;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalProducts = productRepository.count();
        long totalOrders = orderRepository.count();
        long totalUsers = userRepository.count();
        long lowStockCount = productRepository.findAll().stream().filter(p -> p.getStock() < 5).count();

        // Doanh thu tổng (Chỉ đơn hàng DONE)
        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.DONE)
                .map(Order::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Đơn hàng theo trạng thái
        Map<String, Long> ordersByStatus = orderRepository.countOrdersByStatus().stream()
                .collect(Collectors.toMap(
                        row -> ((Order.OrderStatus) row[0]).name(),
                        row -> (Long) row[1]
                ));

        // Doanh thu 30 ngày gần nhất
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<DashboardStatsResponse.RevenueByDate> revenueHistory = orderRepository.getRevenueByDate(thirtyDaysAgo).stream()
                .map(row -> DashboardStatsResponse.RevenueByDate.builder()
                        .date(((java.sql.Date) row[0]).toLocalDate())
                        .revenue((BigDecimal) row[1])
                        .orderCount((Long) row[2])
                        .build())
                .collect(Collectors.toList());

        // Top 5 sản phẩm bán chạy
        List<DashboardStatsResponse.TopProduct> topProducts = orderRepository.getTopSellingProducts(PageRequest.of(0, 5)).stream()
                .map(row -> DashboardStatsResponse.TopProduct.builder()
                        .productId((Long) row[0])
                        .productName((String) row[1])
                        .imageUrl((String) row[2])
                        .totalSold((Long) row[3])
                        .totalRevenue((BigDecimal) row[4])
                        .build())
                .collect(Collectors.toList());

        return DashboardStatsResponse.builder()
                .totalProducts(totalProducts)
                .totalOrders(totalOrders)
                .totalUsers(totalUsers)
                .totalRevenue(totalRevenue)
                .lowStockCount(lowStockCount)
                .ordersByStatus(ordersByStatus)
                .revenueHistory(revenueHistory)
                .topProducts(topProducts)
                .build();
    }
}
