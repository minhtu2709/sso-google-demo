package com.example.demo.controller;

import com.example.demo.entity.Order;
import com.example.demo.entity.OrderItem;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class PaymentController {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @GetMapping("/simulate")
    public String simulatePayment(@RequestParam Long orderId, 
                                  @RequestParam String method,
                                  @RequestParam(required = false) Long createdAt) {
        log.info("Khởi tạo thanh toán giả lập cho đơn hàng #{}, phương thức: {}", orderId, method);
        // Trang giả lập cổng thanh toán
        String url = "redirect:/payment-mock.html?orderId=" + orderId + "&method=" + method;
        if (createdAt != null) {
            url += "&createdAt=" + createdAt;
        }
        return url;
    }

    @GetMapping("/callback")
    @ResponseBody
    @Transactional
    public String handleCallback(@RequestParam Long orderId, @RequestParam boolean success) {
        log.info("Nhận callback thanh toán cho đơn hàng #{}, thành công: {}", orderId, success);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        // Nếu đơn hàng đã hoàn thành hoặc đã hủy trước đó thì không xử lý lại
        if (order.getStatus() == Order.OrderStatus.CANCELLED || order.getStatus() == Order.OrderStatus.DONE) {
             log.warn("Đơn hàng #{} đã ở trạng thái {} trước đó, bỏ qua callback.", orderId, order.getStatus());
             return "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                    "<h2>Thông báo</h2><p>Đơn hàng đã được xử lý trước đó.</p>" +
                    "<a href='/user-dashboard.html'>Quay lại trang chủ</a></body></html>";
        }

        if (success) {
            order.setPaymentStatus("PAID");
            order.setStatus(Order.OrderStatus.CONFIRMED);
            orderRepository.save(order);
            log.info("Cập nhật đơn hàng #{} thành PAID và CONFIRMED", orderId);
            return "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                   "<h2 style='color:green'>✅ Thanh toán thành công!</h2>" +
                   "<p>Đơn hàng #" + orderId + " đã được xác nhận.</p>" +
                   "<p>Bạn có thể đóng trình duyệt này hoặc quay lại ứng dụng.</p>" +
                   "<script>setTimeout(() => { window.location.href='/user-dashboard.html'; }, 3000);</script>" +
                   "</body></html>";
        } else {
            // ROLLBACK: Hủy đơn hàng và hoàn lại tồn kho
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setPaymentStatus("FAILED");
            order.setCancelReason("Thanh toán thất bại hoặc khách hủy thanh toán");
            
            for (OrderItem item : order.getItems()) {
                productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
            }
            orderRepository.save(order);
            log.info("Hủy đơn hàng #{} do thanh toán thất bại/bị hủy", orderId);

            return "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                   "<h2 style='color:red'>❌ Thanh toán thất bại hoặc đã bị hủy</h2>" +
                   "<p>Đơn hàng #" + orderId + " đã bị hủy và số lượng sản phẩm đã được hoàn lại kho.</p>" +
                   "<p>Bạn có thể quay lại giỏ hàng để thực hiện lại.</p>" +
                   "<a href='/user-dashboard.html' style='display:inline-block; margin-top:20px; padding:10px 20px; background:#007bff; color:white; text-decoration:none; border-radius:5px;'>Quay lại cửa hàng</a>" +
                   "</body></html>";
        }
    }

    @GetMapping("/simulate-expire")
    @ResponseBody
    @Transactional
    public String simulateExpire(@RequestParam Long orderId) {
        log.info("Yêu cầu giả lập hết hạn cho đơn hàng #{}", orderId);
        
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng"));

        if (order.getStatus() == Order.OrderStatus.PENDING) {
            // Hoàn lại tồn kho
            for (OrderItem item : order.getItems()) {
                productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
            }
            
            order.setStatus(Order.OrderStatus.CANCELLED);
            order.setPaymentStatus("EXPIRED");
            order.setCancelReason("Hết hạn thanh toán");
            orderRepository.save(order);
            
            log.info("Đơn hàng #{} đã được chuyển sang trạng thái EXPIRED qua giả lập", orderId);
            return "<html><body style='font-family:sans-serif; text-align:center; padding-top:50px;'>" +
                   "<h2 style='color:orange'>🚫 Giả lập hết hạn thành công</h2>" +
                   "<p>Đơn hàng #" + orderId + " đã bị hủy với lý do 'Hết hạn thanh toán'.</p>" +
                   "<script>setTimeout(() => { window.location.href='/user-dashboard.html'; }, 2000);</script>" +
                   "</body></html>";
        }

        return "<html><body><h2>Không thể giả lập: Đơn hàng không ở trạng thái PENDING</h2>" +
               "<a href='/user-dashboard.html'>Quay lại</a></body></html>";
    }
}
