package com.example.demo.service;

import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.entity.Category;
import com.example.demo.entity.Product;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {

        // Tìm category theo id — nếu không có thì báo lỗi ngay
        // Tại sao phải tìm? Vì Product cần biết nó thuộc Category nào
        // không thể chỉ lưu mỗi categoryId số, JPA cần object Category thật
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        // Tạo object Product rồi gán từng field vào
        // Giống như điền form vậy — request là tờ giấy client gửi lên
        // product là bản ghi mình chuẩn bị lưu vào DB
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());
        product.setStatus(request.getStatus());
        product.setCategory(category); // gán object Category, không phải id

        // Lưu vào DB — JPA sẽ tự sinh ra câu INSERT
        // Dùng "saved" chứ không dùng "product" vì sau save()
        // JPA mới gán id tự động vào — nếu dùng product thì id = null!
        Product saved = productRepository.save(product);
        log.info("Tạo sản phẩm thành công: {}", saved.getName());

        // Chuyển Entity → DTO rồi trả về
        // Không trả Product thẳng vì Entity có thể lộ thông tin không cần thiết
        return ProductResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {

        // findAll() → lấy tất cả Product từ DB (dạng List<Product>)
        // .stream() → mở "băng chuyền" xử lý từng phần tử
        // .map(ProductResponse::from) → mỗi Product đi qua băng chuyền
        //                               được convert thành ProductResponse
        // .toList() → gom lại thành List mới
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        return ProductResponse.from(product);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        // Tìm sản phẩm cần sửa — phải tồn tại mới sửa được
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm"));

        // Tìm category mới nếu client muốn đổi danh mục
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));

        // Gán lại toàn bộ thông tin mới vào object cũ
        // JPA tự hiểu đây là UPDATE vì object đã có id rồi
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

        productRepository.delete(product);
        log.info("Đã xóa sản phẩm ID: {}", id);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String keyword) {

        return productRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(ProductResponse::from)
                .toList();
    }
}