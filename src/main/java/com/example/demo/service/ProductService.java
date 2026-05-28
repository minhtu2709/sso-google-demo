package com.example.demo.service;

import com.example.demo.dto.PageMetadata;
import com.example.demo.dto.PageResult;
import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReviewRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(request.getStatus());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        log.info("Tạo sản phẩm thành công: {}", saved.getName());

        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductResponse> getProducts(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String status,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        Sort.Direction dir = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        String field = List.of("name", "price", "createdAt").contains(sortBy) ? sortBy : "id";
        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, field));

        Specification<Product> specification = buildProductSpecification(
                keyword, categoryId, minPrice, maxPrice, status
        );

        Page<Product> productPage = productRepository.findAll(specification, pageable);
        Map<Long, Object[]> reviewSummaryMap = getReviewSummaryMap(
                productPage.getContent().stream().map(Product::getId).toList()
        );

        List<ProductResponse> items = productPage.getContent().stream()
                .map(product -> toProductResponse(product, reviewSummaryMap.get(product.getId())))
                .toList();

        return PageResult.<ProductResponse>builder()
                .items(items)
                .metadata(PageMetadata.from(productPage))
                .build();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        Object[] reviewSummary = getReviewSummaryMap(List.of(product.getId())).get(product.getId());
        return toProductResponse(product, reviewSummary);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(request.getStatus());
        product.setCategory(category);

        Product saved = productRepository.save(product);
        log.info("Cập nhật sản phẩm thành công: {}", saved.getName());

        return ProductResponse.from(saved);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        product.setDeleted(true);
        productRepository.save(product);
        log.info("Đã đánh dấu xóa sản phẩm ID: {}", id);
    }

    @Transactional(readOnly = true)
    public PageResult<ProductResponse> searchProducts(
            String keyword,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        return getProducts(keyword, null, null, null, null, page, size, sortBy, sortDir);
    }

    private Specification<Product> buildProductSpecification(
            String keyword,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String status
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy những sản phẩm chưa bị xóa
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            if (keyword != null && !keyword.isBlank()) {
                String normalizedKeyword = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), normalizedKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), normalizedKeyword)
                ));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("id"), categoryId));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            if (status != null && !status.isBlank()) {
                try {
                    Product.ProductStatus productStatus = Product.ProductStatus.valueOf(status.trim().toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("status"), productStatus));
                } catch (IllegalArgumentException e) {
                    log.warn("Trạng thái không hợp lệ: {}", status);
                }
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Map<Long, Object[]> getReviewSummaryMap(List<Long> productIds) {
        Map<Long, Object[]> summaryMap = new HashMap<>();

        if (productIds.isEmpty()) {
            return summaryMap;
        }

        for (Object[] row : reviewRepository.summarizeByProductIds(productIds)) {
            summaryMap.put((Long) row[0], row);
        }

        return summaryMap;
    }

    private ProductResponse toProductResponse(Product product, Object[] reviewSummary) {
        double averageRating = 0.0;
        long reviewCount = 0L;

        if (reviewSummary != null) {
            averageRating = ((Number) reviewSummary[1]).doubleValue();
            reviewCount = ((Number) reviewSummary[2]).longValue();
        }

        return ProductResponse.from(product, averageRating, reviewCount);
    }
}