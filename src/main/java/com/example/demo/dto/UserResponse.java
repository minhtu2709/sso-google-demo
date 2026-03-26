package com.example.demo.dto;

import com.example.demo.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long id;
    private String email;
    private String name;
    private String provider;


    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider())
                .build();
    }
}