package com.example.demo.service;

import com.example.demo.dto.OrderRequest;
import com.example.demo.dto.OrderResponse;
import com.example.demo.entity.*;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Đặt hàng — chuyển từ Cart sang Order
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest request) {

        // Lấy giỏ hàng của user
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng trống"));

        // Không cho đặt hàng nếu giỏ rỗng
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống, vui lòng thêm sản phẩm");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));

        // Tạo Order mới
        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);
        order.setShippingAddress(request.getShippingAddress());

        // Chuyển từng CartItem → OrderItem
        // Đồng thời tính tổng tiền và trừ tồn kho
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Kiểm tra lại tồn kho lần nữa trước khi đặt
            // Vì từ lúc thêm vào giỏ đến lúc đặt hàng có thể đã hết
            if (product.getStock() < cartItem.getQuantity()) {
                throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' không đủ số lượng trong kho"
                );
            }

            // Tạo OrderItem — lưu giá TẠI THỜI ĐIỂM MUA
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtTime(product.getPrice()); // giá lúc này

            order.getItems().add(orderItem);

            // Cộng vào tổng tiền
            BigDecimal subtotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            totalPrice = totalPrice.add(subtotal);

            // Trừ tồn kho
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        order.setTotalPrice(totalPrice);
        Order saved = orderRepository.save(order);

        // Xóa giỏ hàng sau khi đặt thành công
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Đặt hàng thành công, order ID: {}", saved.getId());
        return OrderResponse.from(saved);
    }

    // Lấy tất cả đơn hàng của user
    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders(Long userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    // Lấy chi tiết 1 đơn hàng
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // Kiểm tra đơn hàng có thuộc về user này không
        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền xem đơn hàng này");
        }

        return OrderResponse.from(order);
    }

    // Hủy đơn hàng — chỉ hủy được khi đang PENDING
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (!order.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Bạn không có quyền hủy đơn hàng này");
        }

        // Chỉ hủy được khi đang PENDING
        // Đã CONFIRMED hoặc SHIPPING thì không hủy được nữa
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalArgumentException("Chỉ có thể hủy đơn hàng đang chờ xác nhận");
        }

        // Hoàn lại tồn kho khi hủy
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        Order saved = orderRepository.save(order);
        log.info("Đã hủy đơn hàng ID: {}", saved.getId());

        return OrderResponse.from(saved);
    }
}