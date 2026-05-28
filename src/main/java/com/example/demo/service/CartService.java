package com.example.demo.service;

import com.example.demo.dto.CartItemRequest;
import com.example.demo.dto.CartResponse;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Thời gian hết hạn của sản phẩm trong giỏ (ví dụ: 24 giờ)
    private static final int CART_EXPIRATION_HOURS = 24;

    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        
        // Logic bổ sung: Kiểm tra tính hợp lệ của từng item trong giỏ
        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();
        var iterator = cart.getItems().iterator();
        
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            Product product = item.getProduct();
            
            // 1. Kiểm tra hết hạn (ví dụ: quá 24h không đặt hàng thì tự xóa khỏi giỏ)
            if (item.getCreatedAt() != null && 
                item.getCreatedAt().plusHours(CART_EXPIRATION_HOURS).isBefore(now)) {
                log.info("Sản phẩm {} trong giỏ đã hết hạn giữ chỗ ({} giờ)", product.getName(), CART_EXPIRATION_HOURS);
                iterator.remove();
                changed = true;
                continue;
            }

            // 2. Nếu sản phẩm không còn ACTIVE -> Xóa khỏi giỏ
            if (product.getStatus() != Product.ProductStatus.ACTIVE) {
                iterator.remove();
                changed = true;
                continue;
            }
            
            // 3. Nếu số lượng trong giỏ > số lượng trong kho -> Giảm xuống bằng tồn kho
            if (item.getQuantity() > product.getStock()) {
                if (product.getStock() <= 0) {
                    iterator.remove();
                } else {
                    item.setQuantity(product.getStock());
                }
                changed = true;
            }
        }
        
        if (changed) {
            cart = cartRepository.save(cart);
        }

        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long cartItemId, int quantity) {
        if (quantity <= 0) {
            return removeItem(userId, cartItemId);
        }

        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền chỉnh sửa sản phẩm này");
        }

        Product product = item.getProduct();
        if (product.getStock() < quantity) {
            throw new IllegalArgumentException("Số lượng yêu cầu vượt quá tồn kho (" + product.getStock() + ")");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {

        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Sản phẩm không đủ số lượng trong kho");
        }

        cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .ifPresentOrElse(
                        existingItem -> {
                            // Kiểm tra tổng số lượng sau khi cộng thêm
                            int newQuantity = existingItem.getQuantity() + request.getQuantity();
                            if (newQuantity > product.getStock()) {
                                throw new IllegalArgumentException("Tổng số lượng sản phẩm trong giỏ vượt quá số lượng trong kho");
                            }
                            // Đã có trong giỏ → tăng số lượng
                            existingItem.setQuantity(newQuantity);
                            cartItemRepository.save(existingItem);
                            log.info("Cập nhật số lượng sản phẩm {} trong giỏ", product.getName());
                        },
                        () -> {
                            // Chưa có → tạo mới
                            CartItem newItem = new CartItem();
                            newItem.setCart(cart);
                            newItem.setProduct(product);
                            newItem.setQuantity(request.getQuantity());
                            
                            // Lưu CartItem trước hoặc để Cascade tự xử lý nhưng phải add vào list
                            cart.getItems().add(newItem);
                            log.info("Thêm sản phẩm {} vào giỏ", product.getName());
                        }
                );

        // Đảm bảo cập nhật lại cart trong database
        cartRepository.save(cart);
        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {

        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ"));

        // Kiểm tra CartItem có thuộc giỏ của user không
        // Tránh user A xóa sản phẩm trong giỏ của user B!
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền xóa sản phẩm này");
        }

        // Xóa khỏi list của cart — orphanRemoval tự xóa trong DB
        cart.getItems().remove(item);
        Cart savedCart = cartRepository.save(cart);
        log.info("Đã xóa sản phẩm khỏi giỏ hàng");

        return CartResponse.from(savedCart);
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        log.info("Đã xóa toàn bộ giỏ hàng của user {}", userId);
    }

    // Tìm giỏ hàng của user, nếu chưa có thì tạo mới
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));

                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }
}