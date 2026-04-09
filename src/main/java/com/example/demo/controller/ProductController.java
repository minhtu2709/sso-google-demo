package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ProductRequest;
import com.example.demo.dto.ProductResponse;
import com.example.demo.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.createProduct(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo sản phẩm thành công", response));
    }

    // GET /products — lấy tất cả sản phẩm
    // List<ProductResponse> vì trả về nhiều sản phẩm
    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {

        List<ProductResponse> response = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    // GET /products/5 — lấy 1 sản phẩm theo id
    // @PathVariable lấy số 5 từ URL gắn vào biến "id"
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {

        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }

    // PUT /products/5 — cập nhật sản phẩm id=5
    // PUT = thay thế toàn bộ, khác PATCH = chỉ sửa 1 phần
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {

        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật thành công", response));
    }

    // DELETE /products/5 — xóa sản phẩm id=5
    // Void vì xóa xong không có data để trả, chỉ có message
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable Long id) {

        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa sản phẩm thành công"));
    }

    // GET /products/search?keyword=áo — tìm kiếm theo tên
    // @RequestParam lấy giá trị từ query string trên URL
    // Khác @PathVariable: PathVariable lấy từ /products/{id}
    //                      RequestParam lấy từ /products/search?keyword=...
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String keyword) {

        List<ProductResponse> response = productService.searchProducts(keyword);
        return ResponseEntity.ok(ApiResponse.success("Thành công", response));
    }
}