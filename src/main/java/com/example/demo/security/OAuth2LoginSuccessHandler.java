package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.entity.RefreshToken;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.RefreshTokenService;
import com.example.demo.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture"); // Link ảnh từ Google

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setName(name);
            newUser.setAvatarUrl(picture);
            newUser.setProvider("GOOGLE");
            
            // Tự động cấp quyền ADMIN nếu là người dùng đầu tiên hoặc email cụ thể
            if (userRepository.count() == 0 || "admin@example.com".equals(email)) {
                newUser.setRole(User.Role.ADMIN);
            } else {
                newUser.setRole(User.Role.USER);
            }
            
            newUser.setEnabled(true);
            newUser.setBlacklisted(false);
            return userRepository.save(newUser);
        });

        // Nếu bạn muốn ép quyền ADMIN cho email của mình, hãy sửa dòng này:
        if ("your-email@gmail.com".equals(email) && user.getRole() != User.Role.ADMIN) {
             user.setRole(User.Role.ADMIN);
             userRepository.save(user);
        }

        // Cập nhật thông tin từ Google nếu cần (Nếu user chưa có tên hoặc avatar)
        boolean needsUpdate = false;
        if ((user.getName() == null || user.getName().isEmpty()) && name != null) {
            user.setName(name);
            needsUpdate = true;
        }
        if (picture != null && (user.getAvatarUrl() == null || user.getAvatarUrl().startsWith("https://ui-avatars.com"))) {
            user.setAvatarUrl(picture);
            needsUpdate = true;
        }

        if (needsUpdate) {
            userRepository.save(user);
        }

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        String targetUrl = "/user-dashboard.html";
        if (user.getRole() == User.Role.ADMIN) {
            targetUrl = "/admin-dashboard.html";
        }

        response.sendRedirect(targetUrl + "#token=" + token + "&refreshToken=" + refreshToken.getToken());
    }
}