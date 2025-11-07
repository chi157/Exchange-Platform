package com.exchange.platform.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class UiAuthController {

    private final com.exchange.platform.repository.UserRepository userRepository;

    // 首頁/教學/Q&A 頁面（未登入也可訪問）
    @GetMapping({"/ui/home", "/ui"})
    public String homePage(HttpSession session, Model model) {
        // 如果已登入，加入使用者資訊
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            String currentUserDisplayName = userRepository.findById(userId)
                    .map(user -> user.getDisplayName())
                    .orElse(null);
            model.addAttribute("currentUserDisplayName", currentUserDisplayName);
            model.addAttribute("isLoggedIn", true);
        } else {
            model.addAttribute("isLoggedIn", false);
        }
        return "home";
    }

    // 根路徑重定向到首頁
    @GetMapping("/")
    public String root(HttpSession session) {
        return "redirect:/ui/home";
    }

    @GetMapping("/ui/auth/login")
    public String loginPage(HttpSession session) {
        // 如果已登入，重定向到首頁
        if (session.getAttribute("userId") != null) {
            return "redirect:/ui/home";
        }
        return "login";
    }

    @GetMapping("/ui/auth/register")
    public String registerPage(HttpSession session) {
        // 如果已登入，重定向到首頁
        if (session.getAttribute("userId") != null) {
            return "redirect:/ui/home";
        }
        return "register";
    }

    @PostMapping("/ui/auth/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/ui/home";
    }
}
