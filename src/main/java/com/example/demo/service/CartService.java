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

    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
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
                            // Đã có trong giỏ → tăng số lượng
                            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
                            cartItemRepository.save(existingItem);
                            log.info("Cập nhật số lượng sản phẩm {} trong giỏ", product.getName());
                        },
                        () -> {
                            // Chưa có → tạo mới và thêm vào list của cart
                            CartItem newItem = new CartItem();
                            newItem.setCart(cart);
                            newItem.setProduct(product);
                            newItem.setQuantity(request.getQuantity());
                            cart.getItems().add(newItem); // thêm vào list cart trước
                            log.info("Thêm sản phẩm {} vào giỏ", product.getName());
                        }
                );

        // Lưu cart — cascade ALL sẽ tự lưu CartItem bên trong
        Cart savedCart = cartRepository.save(cart);
        return CartResponse.from(savedCart);
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