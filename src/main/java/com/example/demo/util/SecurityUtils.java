package com.example.demo.util;

import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserService userService;

    public Long getCurrentUserId(Object principal) {
        String email = extractEmail(principal);
        return userService.getUserByEmail(email).getId();
    }

    public String extractEmail(Object principal) {
        if (principal == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                throw new IllegalArgumentException("Chua dang nhap");
            }

            return authentication.getName();
        }

        if (principal instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof Authentication authentication) {
            return authentication.getName();
        }

        if (principal instanceof String username) {
            return username;
        }

        throw new IllegalArgumentException("Chua dang nhap");
    }
}
