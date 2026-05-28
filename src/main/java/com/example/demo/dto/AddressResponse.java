package com.example.demo.dto;

import com.example.demo.entity.Address;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AddressResponse {
    private Long id;
    private String recipientName;
    private String phoneNumber;
    private String provinceName;
    private String districtName;
    private String wardName;
    private String provinceCode;
    private String districtCode;
    private String wardCode;
    private String detailAddress;
    private boolean isDefault;

    public static AddressResponse fromEntity(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .provinceName(address.getProvinceName())
                .districtName(address.getDistrictName())
                .wardName(address.getWardName())
                .provinceCode(address.getProvinceCode())
                .districtCode(address.getDistrictCode())
                .wardCode(address.getWardCode())
                .detailAddress(address.getDetailAddress())
                .isDefault(address.isDefault())
                .build();
    }
}
