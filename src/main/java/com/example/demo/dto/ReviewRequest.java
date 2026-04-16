package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewRequest {

    @Min(value = 1, message = "Rating phai tu 1 den 5")
    @Max(value = 5, message = "Rating phai tu 1 den 5")
    private Integer rating;

    @Size(max = 1000, message = "Noi dung review khong duoc vuot qua 1000 ky tu")
    private String comment;
}