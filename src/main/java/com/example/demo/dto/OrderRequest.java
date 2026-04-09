package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderRequest {

    @NotBlank(message = "Địa chỉ giao hàng không được để trống")
    private String shippingAddress;
}