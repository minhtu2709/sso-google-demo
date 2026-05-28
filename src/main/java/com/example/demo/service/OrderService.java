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
    private final com.example.demo.repository.AddressRepository addressRepository;

    // Đặt hàng — chuyển từ Cart sang Order
    @Transactional
    public OrderResponse placeOrder(Long userId, OrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));

        // 1. Kiểm tra Blacklist
        if (user.isBlacklisted()) {
            throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa tính năng đặt hàng do vi phạm quy định.");
        }

        // Tự động kích hoạt tài khoản nếu chưa được kích hoạt (trường hợp user cũ chưa có giá trị enabled)
        if (!user.isEnabled()) {
            user.setEnabled(true);
            userRepository.save(user);
        }

        // Lấy giỏ hàng của user - Dùng LOCK để tránh đặt hàng trùng lặp (Concurrency)
        Cart cart = cartRepository.findWithLockByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Giỏ hàng trống"));

        // Không cho đặt hàng nếu giỏ rỗng (Quan trọng khi có 2 request cùng lúc)
        if (cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Giỏ hàng đang trống, vui lòng thêm sản phẩm");
        }

        // Tạo Order mới
        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.OrderStatus.PENDING);

        // Xử lý địa chỉ chuẩn chỉ
        if (request.getAddressId() != null) {
            Address address = addressRepository.findByIdAndUser(request.getAddressId(), user)
                    .orElseThrow(() -> new IllegalArgumentException("Địa chỉ giao hàng không hợp lệ"));

            // Snapshot thông tin địa chỉ
            order.setRecipientName(address.getRecipientName());
            order.setRecipientPhone(address.getPhoneNumber());
            order.setProvince(address.getProvinceName());
            order.setDistrict(address.getDistrictName());
            order.setWard(address.getWardName());
            order.setDetailAddress(address.getDetailAddress());

            // CHUẨN: shippingAddress chỉ chứa địa chỉ, không chứa Tên hay SĐT vì đã có cột riêng
            String fullAddress = String.format("%s, %s, %s, %s",
                    address.getDetailAddress(), address.getWardName(), address.getDistrictName(), address.getProvinceName());
            order.setShippingAddress(fullAddress);
        } else if (request.getShippingAddress() != null && !request.getShippingAddress().isBlank()) {
            order.setShippingAddress(request.getShippingAddress());
            // Fallback thông tin người nhận từ Profile nếu không có địa chỉ cụ thể
            order.setRecipientName(user.getName());
            order.setRecipientPhone(user.getPhoneNumber());
        } else {
            // CỰC KỲ QUAN TRỌNG: Nếu không có addressId, hãy thử lấy địa chỉ từ Profile User
            if (user.getAddress() != null && !user.getAddress().isBlank()) {
                order.setShippingAddress(user.getAddress());
                order.setRecipientName(user.getName());
                order.setRecipientPhone(user.getPhoneNumber());
                log.info("Sử dụng địa chỉ từ profile cho user: {}", user.getEmail());
            } else {
                throw new IllegalArgumentException("Vui lòng chọn địa chỉ giao hàng hoặc cập nhật địa chỉ trong hồ sơ.");
            }
        }

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

            // Trừ tồn kho ATOMIC - Chống tranh chấp (Concurrency)
            int rowsAffected = productRepository.decreaseStock(product.getId(), cartItem.getQuantity());
            if (rowsAffected == 0) {
                throw new IllegalArgumentException(
                        "Sản phẩm '" + product.getName() + "' vừa hết hàng hoặc không đủ số lượng. Vui lòng kiểm tra lại giỏ hàng."
                );
            }
        }

        order.setTotalPrice(totalPrice);
        
        // Thiết lập phương thức thanh toán (Ưu tiên từ Request, mặc định là COD)
        String method = request.getPaymentMethod();
        if (method == null || method.trim().isEmpty()) {
            method = "COD";
        }
        order.setPaymentMethod(method.toUpperCase());

        if ("COD".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("UNPAID");
        } else {
            order.setPaymentStatus("UNPAID"); // Đợi thanh toán online/bank
        }

        Order saved = orderRepository.save(order);

        // Xóa giỏ hàng sau khi đặt thành công
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Đặt hàng thành công, order ID: {}, Method: {}", saved.getId(), saved.getPaymentMethod());
        
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
    public OrderResponse cancelOrder(Long orderId, Long userId, String reason) {
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
            productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
        }

        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancelReason(reason != null ? reason : "Người dùng yêu cầu hủy");
        
        if (!"PAID".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("CANCELLED");
        }
        Order saved = orderRepository.save(order);
        log.info("Đã hủy đơn hàng ID: {} - Lý do: {}", saved.getId(), order.getCancelReason());

        return OrderResponse.from(saved);
    }

    // Overload for backward compatibility if needed or internal use
    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        return cancelOrder(orderId, userId, "Người dùng yêu cầu hủy");
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, String status, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status);
            Order.OrderStatus oldStatus = order.getStatus();

            if (newStatus == oldStatus) {
                return OrderResponse.from(order);
            }

            // Chặn chuyển trạng thái nếu đơn hàng đã bị hủy hoặc đã hoàn thành
            if (oldStatus == Order.OrderStatus.CANCELLED) {
                throw new IllegalArgumentException("Đơn hàng đã bị hủy, không thể thay đổi trạng thái.");
            }
            if (oldStatus == Order.OrderStatus.DONE) {
                throw new IllegalArgumentException("Đơn hàng đã hoàn thành, không thể thay đổi trạng thái.");
            }

            // Xử lý khi chuyển sang CANCELLED
            if (newStatus == Order.OrderStatus.CANCELLED) {
                // Hoàn lại tồn kho
                for (OrderItem item : order.getItems()) {
                    productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
                }
                // Cập nhật trạng thái thanh toán nếu chưa trả tiền
                if (!"PAID".equals(order.getPaymentStatus())) {
                    order.setPaymentStatus("CANCELLED");
                }
                // Cập nhật lý do hủy
                order.setCancelReason(reason != null ? reason : "Admin hủy đơn hàng");
            }

            // Tự động chuyển trạng thái thanh toán thành PAID nếu đơn hàng hoàn thành
            if (newStatus == Order.OrderStatus.DONE) {
                order.setPaymentStatus("PAID");
            }

            order.setStatus(newStatus);
            return OrderResponse.from(orderRepository.save(order));
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("No enum constant")) {
                throw new IllegalArgumentException("Trạng thái đơn hàng không hợp lệ: " + status);
            }
            throw e;
        }
    }

    // Overload for updateStatus
    @Transactional
    public OrderResponse updateStatus(Long orderId, String status) {
        return updateStatus(orderId, status, null);
    }
}