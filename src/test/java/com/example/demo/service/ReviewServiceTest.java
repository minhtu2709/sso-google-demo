package com.example.demo.service;

import com.example.demo.dto.ReviewRequest;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.entity.Product;
import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User user;
    private Product product;
    private ReviewRequest request;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);

        product = new Product();
        product.setId(1L);
        product.setName("Test Product");

        request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Great product!");
    }

    @Test
    void upsertReview_shouldThrowException_whenUserHasNotPurchasedProduct() {
        // Arrange
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.hasPurchasedProduct(1L, 1L)).thenReturn(false);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> reviewService.upsertReview(1L, 1L, request));
    }

    @Test
    void upsertReview_shouldSucceed_whenUserHasPurchasedProduct() {
        // Arrange
        Review existingReview = new Review();
        existingReview.setId(1L);
        existingReview.setProduct(product);
        existingReview.setUser(user);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(orderRepository.hasPurchasedProduct(1L, 1L)).thenReturn(true);
        when(reviewRepository.findByProductIdAndUserId(1L, 1L)).thenReturn(Optional.of(existingReview));
        when(reviewRepository.save(any(Review.class))).thenReturn(existingReview);

        // Act
        ReviewResponse response = reviewService.upsertReview(1L, 1L, request);

        // Assert
        org.junit.jupiter.api.Assertions.assertNotNull(response);
    }
}
