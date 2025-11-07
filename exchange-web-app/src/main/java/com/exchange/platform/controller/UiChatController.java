package com.exchange.platform.controller;

import com.exchange.platform.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ui/chat")
@RequiredArgsConstructor
public class UiChatController {

    private final UserRepository userRepository;

    @GetMapping
    public String chatPage(HttpSession session, Model model) {
        // 檢查是否登入
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/ui/auth/login";
        }

        // 獲取當前用戶資訊
        userRepository.findById(userId).ifPresent(user -> {
            model.addAttribute("currentUserId", user.getId());
            model.addAttribute("currentUserDisplayName", user.getDisplayName());
        });

        return "chat";
    }
}
