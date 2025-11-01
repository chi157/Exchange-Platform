package com.exchange.platform.controller;

import com.exchange.platform.dto.SwapDTO;
import com.exchange.platform.service.SwapService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/ui/swaps")
@RequiredArgsConstructor
public class UiSwapController {

    private final SwapService swapService;

    @GetMapping("/mine")
    public String mine(@RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       @RequestParam(required = false) String sort,
                       HttpSession session,
                       Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }

        Integer pageArg = (page == null || page <= 0) ? 1 : page;
        Integer sizeArg = (size == null || size <= 0) ? 10 : Math.min(size, 100);
        String sortArg = (sort == null || sort.isBlank()) ? "createdAt,DESC" : sort;

        List<SwapDTO> items = swapService.listMine(session, pageArg, sizeArg, sortArg);

        model.addAttribute("items", items);
        model.addAttribute("page", pageArg);
        model.addAttribute("size", sizeArg);
        model.addAttribute("sort", sortArg);
        return "swaps";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
                        HttpSession session,
                        Model model) {
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }

        SwapDTO swap = swapService.getById(id, session);
        Long currentUserId = (Long) session.getAttribute("userId");
        
        model.addAttribute("swap", swap);
        model.addAttribute("currentUserId", currentUserId);
        return "swap-detail";
    }
}

