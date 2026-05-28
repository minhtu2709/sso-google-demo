package com.example.demo.service;

import com.example.demo.dto.AddressRequest;
import com.example.demo.dto.AddressResponse;
import com.example.demo.entity.Address;
import com.example.demo.entity.User;
import com.example.demo.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;

    public List<AddressResponse> getMyAddresses(User user) {
        return addressRepository.findByUser(user).stream()
                .map(AddressResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse addAddress(User user, AddressRequest request) {
        // Nếu là địa chỉ mặc định, bỏ các mặc định cũ
        if (request.isDefault()) {
            clearDefaultAddress(user);
        }

        // Nếu là địa chỉ đầu tiên, tự động cho làm mặc định
        boolean isFirst = addressRepository.findByUser(user).isEmpty();

        Address address = Address.builder()
                .recipientName(request.getRecipientName())
                .phoneNumber(request.getPhoneNumber())
                .provinceName(request.getProvinceName())
                .districtName(request.getDistrictName())
                .wardName(request.getWardName())
                .provinceCode(request.getProvinceCode())
                .districtCode(request.getDistrictCode())
                .wardCode(request.getWardCode())
                .detailAddress(request.getDetailAddress())
                .isDefault(request.isDefault() || isFirst)
                .user(user)
                .build();

        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(User user, Long id, AddressRequest request) {
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại"));

        if (request.isDefault() && !address.isDefault()) {
            clearDefaultAddress(user);
        }

        address.setRecipientName(request.getRecipientName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setProvinceName(request.getProvinceName());
        address.setDistrictName(request.getDistrictName());
        address.setWardName(request.getWardName());
        address.setProvinceCode(request.getProvinceCode());
        address.setDistrictCode(request.getDistrictCode());
        address.setWardCode(request.getWardCode());
        address.setDetailAddress(request.getDetailAddress());
        address.setDefault(request.isDefault());

        return AddressResponse.fromEntity(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(User user, Long id) {
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại"));
        
        if (address.isDefault()) {
            throw new RuntimeException("Không thể xóa địa chỉ mặc định");
        }
        
        addressRepository.delete(address);
    }

    @Transactional
    public void setDefaultAddress(User user, Long id) {
        clearDefaultAddress(user);
        Address address = addressRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại"));
        address.setDefault(true);
        addressRepository.save(address);
    }

    private void clearDefaultAddress(User user) {
        addressRepository.findFirstByUserAndIsDefaultTrue(user)
                .ifPresent(a -> {
                    a.setDefault(false);
                    addressRepository.save(a);
                });
    }
}
