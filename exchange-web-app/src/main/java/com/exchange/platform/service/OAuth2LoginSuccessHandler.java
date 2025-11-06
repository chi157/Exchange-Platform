package com.exchange.platform.service;

import com.exchange.platform.entity.User;
import com.exchange.platform.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private static final String SESSION_USER_ID = "userId";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                       Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oauth2User.getAttributes();
        
        // 從 Google OAuth2 獲取 email
        String email = (String) attributes.get("email");
        String oauth2Id = (String) attributes.get("sub");
        
        log.info("OAuth2 登入成功 - Email: {}, OAuth2 ID: {}", email, oauth2Id);
        
        // 從資料庫找到用戶並設置 session
        Optional<User> userOpt = userRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            HttpSession session = request.getSession();
            session.setAttribute(SESSION_USER_ID, user.getId());
            
            log.info("已設置 session，用戶 ID: {}", user.getId());
            
            // 重定向到主頁或用戶儀表板
            response.sendRedirect("/ui/listings");
        } else {
            log.error("找不到 OAuth2 用戶: {}", email);
            response.sendRedirect("/ui/auth/login?error=user_not_found");
        }
    }
}
