package com.example.demo.dto;

import com.example.demo.entity.User;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RoleUpdateRequest {

    @NotNull(message = "Role không được để trống")
    private User.Role role;
}