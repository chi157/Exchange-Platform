package com.exchange.platform.controller;

import com.exchange.platform.dto.AuthResponse;
import com.exchange.platform.dto.UpdateProfileRequest;
import com.exchange.platform.dto.UserDTO;
import com.exchange.platform.repository.UserRepository;
import com.exchange.platform.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/ui/profile")
@RequiredArgsConstructor
@Slf4j
public class UiProfileController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @GetMapping
    public String profile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/ui/auth/login";
        }

        UserDTO currentUser = authService.getCurrentUser(session);
        model.addAttribute("user", currentUser);
        
        String currentUserDisplayName = userRepository.findById(userId)
                .map(u -> u.getDisplayName())
                .orElse("Unknown");
        model.addAttribute("currentUserDisplayName", currentUserDisplayName);

        return "profile";
    }

    @PostMapping("/update")
    public String updateProfile(
            HttpSession session,
            @ModelAttribute UpdateProfileRequest request,
            RedirectAttributes redirectAttributes) {
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/ui/auth/login";
        }

        AuthResponse response = authService.updateProfile(userId, request);
        
        if (response.isSuccess()) {
            redirectAttributes.addFlashAttribute("success", response.getMessage());
        } else {
            redirectAttributes.addFlashAttribute("error", response.getMessage());
        }

        return "redirect:/ui/profile";
    }
}
