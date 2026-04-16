package com.example.demo.service;

import com.example.demo.dto.PageMetadata;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.ReviewRequest;
import com.example.demo.dto.ReviewResponse;
import com.example.demo.entity.Product;
import com.example.demo.entity.Review;
import com.example.demo.entity.User;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewResponse upsertReview(Long productId, Long userId, ReviewRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay san pham"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay user"));

        Review review = reviewRepository.findByProductIdAndUserId(productId, userId)
                .orElseGet(Review::new);

        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review saved = reviewRepository.save(review);
        log.info("Review da duoc luu cho productId={}, userId={}", productId, userId);

        return ReviewResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResult<ReviewResponse> getReviewsByProductId(
            Long productId,
            int page,
            int size,
            String sortDir) {

        if (!productRepository.existsById(productId)) {
            throw new IllegalArgumentException("Khong tim thay san pham");
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "createdAt"));
        Page<Review> reviewPage = reviewRepository.findByProductId(productId, pageable);

        List<ReviewResponse> items = reviewPage.getContent().stream()
                .map(ReviewResponse::from)
                .toList();

        return PageResult.<ReviewResponse>builder()
                .items(items)
                .metadata(PageMetadata.from(reviewPage))
                .build();
    }
}