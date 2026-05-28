package com.example.demo.controller;

import com.example.demo.dto.AddressRequest;
import com.example.demo.dto.ApiResponse;
import com.example.demo.entity.User;
import com.example.demo.service.AddressService;
import com.example.demo.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Address", description = "Quản lý địa chỉ giao hàng")
public class AddressController {

    private final AddressService addressService;
    private final SecurityUtils securityUtils;

    @GetMapping
    @Operation(summary = "Lấy danh sách địa chỉ của tôi")
    public ApiResponse<?> getMyAddresses() {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.success("Lấy danh sách thành công", addressService.getMyAddresses(user));
    }

    @PostMapping
    @Operation(summary = "Thêm địa chỉ mới")
    public ApiResponse<?> addAddress(@Valid @RequestBody AddressRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.success("Thêm địa chỉ thành công", addressService.addAddress(user, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Cập nhật địa chỉ")
    public ApiResponse<?> updateAddress(@PathVariable Long id, @Valid @RequestBody AddressRequest request) {
        User user = securityUtils.getCurrentUser();
        return ApiResponse.success("Cập nhật địa chỉ thành công", addressService.updateAddress(user, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa địa chỉ")
    public ApiResponse<?> deleteAddress(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        addressService.deleteAddress(user, id);
        return ApiResponse.success("Đã xóa địa chỉ");
    }

    @PatchMapping("/{id}/default")
    @Operation(summary = "Đặt làm địa chỉ mặc định")
    public ApiResponse<?> setDefault(@PathVariable Long id) {
        User user = securityUtils.getCurrentUser();
        addressService.setDefaultAddress(user, id);
        return ApiResponse.success("Đã đặt làm mặc định");
    }
}
