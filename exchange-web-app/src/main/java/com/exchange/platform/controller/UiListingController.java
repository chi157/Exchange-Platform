package com.exchange.platform.controller;

import com.exchange.platform.dto.ListingDTO;
import com.exchange.platform.service.ListingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/ui")
@RequiredArgsConstructor
public class UiListingController {

    private final ListingService listingService;

    // Home redirect to listings
    @GetMapping({"", "/"})
    public String home(HttpSession session) {
        // 如果未登入，重定向到登入頁
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }
        return "redirect:/ui/listings";
    }

    // M1: 搜尋/分頁 畫面
    @GetMapping("/listings")
    public String listings(@RequestParam(required = false) Integer page,
                           @RequestParam(required = false) Integer size,
                           @RequestParam(required = false) String q,
                           @RequestParam(required = false) String sort,
                           HttpSession session,
                           Model model) {
        // 如果未登入，重定向到登入頁
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }

        // 預設值（與後端 API 對齊：page 1 起算、size 預設 5）
        Integer pageArg = (page == null || page <= 0) ? 1 : page;
        Integer sizeArg = (size == null || size <= 0) ? 5 : Math.min(size, 100);
        String sortArg = (sort == null || sort.isBlank()) ? "createdAt,DESC" : sort;

        Page<ListingDTO> pageResult = listingService.listPage(pageArg, sizeArg, q, sortArg);

        model.addAttribute("items", pageResult.getContent());
        model.addAttribute("page", pageArg);
        model.addAttribute("size", sizeArg);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("sort", sortArg);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalElements", pageResult.getTotalElements());
        model.addAttribute("currentUserId", session.getAttribute("userId"));
        return "listings";
    }
}
