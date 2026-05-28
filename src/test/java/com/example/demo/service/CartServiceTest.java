package com.example.demo.service;

import com.example.demo.dto.CartItemRequest;
import com.example.demo.entity.Cart;
import com.example.demo.entity.CartItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.User;
import com.example.demo.repository.CartItemRepository;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartServiceTest {

    @Mock
    private CartRepository cartRepository;
    @Mock
    private CartItemRepository cartItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private User user;
    private Cart cart;
    private Product product;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        cart = new Cart();
        cart.setId(1L);
        cart.setUser(user);

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setStock(10);
    }

    @Test
    void addItem_shouldThrowException_whenTotalQuantityExceedsStock() {
        // Arrange
        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(6);

        CartItem existingItem = new CartItem();
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(5);

        when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.of(existingItem));

        // Act & Assert
        // existingItem.quantity(5) + request.quantity(6) = 11 > product.stock(10)
        assertThrows(IllegalArgumentException.class, () -> cartService.addItem(1L, request));
    }

    @Test
    void addItem_shouldSucceed_whenTotalQuantityWithinStock() {
        // Arrange
        CartItemRequest request = new CartItemRequest();
        request.setProductId(1L);
        request.setQuantity(3);

        CartItem existingItem = new CartItem();
        existingItem.setCart(cart);
        existingItem.setProduct(product);
        existingItem.setQuantity(5);

        when(cartRepository.findByUserId(anyLong())).thenReturn(Optional.of(cart));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId())).thenReturn(Optional.of(existingItem));
        when(cartRepository.save(cart)).thenReturn(cart);

        // Act
        // existingItem.quantity(5) + request.quantity(3) = 8 <= product.stock(10)
        cartService.addItem(1L, request);
    }
}
