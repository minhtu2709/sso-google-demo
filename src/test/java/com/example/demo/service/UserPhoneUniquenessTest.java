package com.example.demo.service;

import com.example.demo.dto.ProfileUpdateRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class UserPhoneUniquenessTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        // We cannot delete all users because of foreign key constraints with carts/orders/etc.
        // Instead, we will use unique emails for each test run if needed,
        // but since we delete all users at the start, we need to be careful.
        // For testing, let's just use what we have or try to clean up if possible.
    }

    @Test
    void testUpdateProfileWithDuplicatePhoneNumberShouldFail() {
        String email1 = "user1_" + System.currentTimeMillis() + "@example.com";
        String email2 = "user2_" + System.currentTimeMillis() + "@example.com";
        String phone = "09" + String.format("%08d", (int)(Math.random() * 100000000));

        // Create user 1
        User user1 = new User();
        user1.setEmail(email1);
        user1.setPhoneNumber(phone);
        user1.setName("User One");
        user1.setRole(User.Role.USER);
        user1.setEnabled(true);
        user1.setBlacklisted(false);
        user1.setProvider("LOCAL");
        userRepository.save(user1);

        // Create user 2
        User user2 = new User();
        user2.setEmail(email2);
        user2.setPhoneNumber("0987654321");
        user2.setName("User Two");
        user2.setRole(User.Role.USER);
        user2.setEnabled(true);
        user2.setBlacklisted(false);
        user2.setProvider("LOCAL");
        userRepository.save(user2);

        // Try to update user 2 with user 1's phone number
        ProfileUpdateRequest updateRequest = new ProfileUpdateRequest();
        updateRequest.setName("User Two Updated");
        updateRequest.setPhoneNumber(phone); // Same as user 1
        updateRequest.setAddress("Some address");

        assertThrows(IllegalArgumentException.class, () -> {
            userService.updateProfile(email2, updateRequest);
        }, "Should throw exception when phone number is already taken");
    }
}
