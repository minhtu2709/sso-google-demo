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

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // Lấy giỏ hàng của user
    // Nếu chưa có thì tự tạo mới — không bắt user phải "tạo giỏ hàng" thủ công
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return CartResponse.from(cart);
    }

    // Thêm sản phẩm vào giỏ
    @Transactional
    public CartResponse addItem(Long userId, CartItemRequest request) {

        Cart cart = getOrCreateCart(userId);

        // Kiểm tra sản phẩm có tồn tại không
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        // Kiểm tra còn hàng không
        if (product.getStock() < request.getQuantity()) {
            throw new IllegalArgumentException("Sản phẩm không đủ số lượng trong kho");
        }

        // Tìm xem sản phẩm đã có trong giỏ chưa
        // Nếu có rồi → tăng số lượng, không tạo dòng mới
        // Nếu chưa → tạo CartItem mới
        cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())
                .ifPresentOrElse(
                        existingItem -> {
                            // Sản phẩm đã có trong giỏ → cộng thêm số lượng
                            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
                            cartItemRepository.save(existingItem);
                            log.info("Cập nhật số lượng sản phẩm {} trong giỏ", product.getName());
                        },
                        () -> {
                            // Sản phẩm chưa có trong giỏ → tạo mới
                            CartItem newItem = new CartItem();
                            newItem.setCart(cart);
                            newItem.setProduct(product);
                            newItem.setQuantity(request.getQuantity());
                            cartItemRepository.save(newItem);
                            log.info("Thêm sản phẩm {} vào giỏ", product.getName());
                        }
                );

        // Trả về giỏ hàng mới nhất sau khi thêm
        Cart updatedCart = cartRepository.findById(cart.getId()).get();
        return CartResponse.from(updatedCart);
    }

    // Xóa 1 sản phẩm khỏi giỏ
    @Transactional
    public CartResponse removeItem(Long userId, Long cartItemId) {

        Cart cart = getOrCreateCart(userId);

        // Tìm CartItem cần xóa
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ"));

        // Kiểm tra CartItem này có thuộc giỏ của user không
        // Tránh user A xóa sản phẩm trong giỏ của user B!
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Bạn không có quyền xóa sản phẩm này");
        }

        cartItemRepository.delete(item);
        log.info("Đã xóa sản phẩm khỏi giỏ hàng");

        Cart updatedCart = cartRepository.findById(cart.getId()).get();
        return CartResponse.from(updatedCart);
    }

    // Xóa toàn bộ giỏ hàng
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear(); // orphanRemoval = true sẽ tự xóa trong DB
        cartRepository.save(cart);
        log.info("Đã xóa toàn bộ giỏ hàng của user {}", userId);
    }

    // Helper method — tìm hoặc tạo giỏ hàng
    // Dùng private vì chỉ dùng nội bộ trong Service này
    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Chưa có giỏ → tạo mới
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user"));

                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return cartRepository.save(newCart);
                });
    }
}