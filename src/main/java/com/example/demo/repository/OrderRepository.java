package com.example.demo.repository;

import com.example.demo.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // Lấy tất cả đơn hàng của 1 user
    // 1 user có nhiều order nên dùng List
    List<Order> findByUserId(Long userId);

    // Lấy các đơn hàng PENDING đã quá hạn
    List<Order> findByStatusAndCreatedAtBefore(Order.OrderStatus status, java.time.LocalDateTime dateTime);

    @Query("SELECT COUNT(o) > 0 FROM Order o JOIN o.items i " +
           "WHERE o.user.id = :userId AND i.product.id = :productId AND o.status = com.example.demo.entity.Order.OrderStatus.DONE")
    boolean hasPurchasedProduct(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersByStatus();

    @Query("SELECT CAST(o.createdAt AS date) as date, SUM(o.totalPrice), COUNT(o) " +
           "FROM Order o " +
           "WHERE o.status = com.example.demo.entity.Order.OrderStatus.DONE " +
           "AND o.createdAt >= :startDate " +
           "GROUP BY CAST(o.createdAt AS date) " +
           "ORDER BY date ASC")
    List<Object[]> getRevenueByDate(@Param("startDate") java.time.LocalDateTime startDate);

    @Query("SELECT i.product.id, i.product.name, i.product.imageUrl, SUM(i.quantity), SUM(i.priceAtTime * i.quantity) " +
           "FROM OrderItem i " +
           "WHERE i.order.status = com.example.demo.entity.Order.OrderStatus.DONE " +
           "GROUP BY i.product.id, i.product.name, i.product.imageUrl " +
           "ORDER BY SUM(i.quantity) DESC")
    List<Object[]> getTopSellingProducts(org.springframework.data.domain.Pageable pageable);
}
