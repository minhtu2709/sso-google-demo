package com.example.demo.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardStatsResponse {
    private long totalProducts;
    private long totalOrders;
    private long totalUsers;
    private BigDecimal totalRevenue;
    private long lowStockCount;

    private Map<String, Long> ordersByStatus;
    private List<RevenueByDate> revenueHistory;
    private List<TopProduct> topProducts;

    @Data
    @Builder
    public static class RevenueByDate {
        private LocalDate date;
        private BigDecimal revenue;
        private long orderCount;
    }

    @Data
    @Builder
    public static class TopProduct {
        private Long productId;
        private String productName;
        private String imageUrl;
        private long totalSold;
        private BigDecimal totalRevenue;
    }
}
