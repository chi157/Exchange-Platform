package com.exchange.platform.controller;

import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.service.ProposalService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/ui/proposals")
@RequiredArgsConstructor
public class UiProposalController {

    private final ProposalService proposalService;
    private final com.exchange.platform.repository.UserRepository userRepository;

    // M2: 我提出的提案
    @GetMapping("/mine")
    public String mine(@RequestParam(required = false) Integer page,
                       @RequestParam(required = false) Integer size,
                       @RequestParam(required = false) String sort,
                       HttpSession session,
                       Model model) {
        // 如果未登入，重定向到登入頁
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }

        Integer pageArg = (page == null || page <= 0) ? 1 : page;
        Integer sizeArg = (size == null || size <= 0) ? 10 : Math.min(size, 100);
        String sortArg = (sort == null || sort.isBlank()) ? "createdAt,DESC" : sort;

        List<ProposalDTO> items = proposalService.listMine(session, pageArg, sizeArg, sortArg);

        model.addAttribute("items", items);
        model.addAttribute("page", pageArg);
        model.addAttribute("size", sizeArg);
        model.addAttribute("sort", sortArg);
        model.addAttribute("viewType", "mine");
        
        // 加入當前使用者的顯示名稱
        Long userId = (Long) session.getAttribute("userId");
        String currentUserDisplayName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("訪客");
        model.addAttribute("currentUserDisplayName", currentUserDisplayName);
        
        return "proposals";
    }

    // M2: 我收到的提案
    @GetMapping("/received")
    public String received(@RequestParam(required = false) Integer page,
                          @RequestParam(required = false) Integer size,
                          @RequestParam(required = false) String sort,
                          HttpSession session,
                          Model model) {
        // 如果未登入，重定向到登入頁
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }

        Integer pageArg = (page == null || page <= 0) ? 1 : page;
        Integer sizeArg = (size == null || size <= 0) ? 10 : Math.min(size, 100);
        String sortArg = (sort == null || sort.isBlank()) ? "createdAt,DESC" : sort;

        List<ProposalDTO> items = proposalService.listReceived(session, pageArg, sizeArg, sortArg);

        model.addAttribute("items", items);
        model.addAttribute("page", pageArg);
        model.addAttribute("size", sizeArg);
        model.addAttribute("sort", sortArg);
        model.addAttribute("viewType", "received");
        
        // 加入當前使用者的顯示名稱
        Long userId = (Long) session.getAttribute("userId");
        String currentUserDisplayName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("訪客");
        model.addAttribute("currentUserDisplayName", currentUserDisplayName);
        
        return "proposals";
    }
}
