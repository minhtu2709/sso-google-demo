package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank(message = "Tên người nhận không được để trống")
    private String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^(0|\\+84)(3|5|7|8|9)[0-9]{8}$", message = "Số điện thoại không hợp lệ (phải là số VN, 10 chữ số nếu bắt đầu bằng 0)")
    private String phoneNumber;

    @NotBlank(message = "Tỉnh/Thành phố không được để trống")
    private String provinceName;

    @NotBlank(message = "Quận/Huyện không được để trống")
    private String districtName;

    @NotBlank(message = "Phường/Xã không được để trống")
    private String wardName;
    
    private String provinceCode;
    private String districtCode;
    private String wardCode;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    private String detailAddress;
    
    private boolean isDefault;
}
