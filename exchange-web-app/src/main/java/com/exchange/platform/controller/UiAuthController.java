package com.exchange.platform.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class UiAuthController {

    // 根路徑重定向
    @GetMapping("/")
    public String root(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }
        return "redirect:/ui/listings";
    }

    @GetMapping("/ui/auth/login")
    public String loginPage(HttpSession session) {
        // 如果已登入，重定向到首頁
        if (session.getAttribute("userId") != null) {
            return "redirect:/ui/listings";
        }
        return "login";
    }

    @GetMapping("/ui/auth/register")
    public String registerPage(HttpSession session) {
        // 如果已登入，重定向到首頁
        if (session.getAttribute("userId") != null) {
            return "redirect:/ui/listings";
        }
        return "register";
    }

    @PostMapping("/ui/auth/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/ui/auth/login";
    }
}
