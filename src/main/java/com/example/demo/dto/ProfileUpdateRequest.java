package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfileUpdateRequest {

    @NotBlank(message = "Tên không được để trống")
    private String name;

    @NotBlank(message = "Số điện thoại không được để trống")
    @jakarta.validation.constraints.Pattern(regexp = "^[0-9]{10}$", message = "Số điện thoại phải gồm 10 chữ số")
    private String phoneNumber;

    @NotBlank(message = "Địa chỉ không được để trống")
    private String address;
}
