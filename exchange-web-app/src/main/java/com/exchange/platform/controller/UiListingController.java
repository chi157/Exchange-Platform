package com.exchange.platform.controller;

import com.exchange.platform.dto.ListingDTO;
import com.exchange.platform.service.ListingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/ui")
@RequiredArgsConstructor
public class UiListingController {

    private final ListingService listingService;
    private final com.exchange.platform.repository.UserRepository userRepository;

    // Home redirect to listings
    @GetMapping({"", "/"})
    public String home(HttpSession session) {
        // 如果未登入，重定向到登入頁
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }
        return "redirect:/ui/listings";
    }

    // M1: 搜尋/分頁 畫面（排除自己的刊登）
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

        // 預設值（與後端 API 對齊：page 1 起算、size 預設 6）
        Integer pageArg = (page == null || page <= 0) ? 1 : page;
        Integer sizeArg = (size == null || size <= 0) ? 6 : Math.min(size, 100);
        String sortArg = (sort == null || sort.isBlank()) ? "createdAt,DESC" : sort;

        Long userId = (Long) session.getAttribute("userId");
        // 排除當前使用者的刊登
        Page<ListingDTO> pageResult = listingService.listPage(pageArg, sizeArg, q, sortArg, userId);

        model.addAttribute("items", pageResult.getContent());
        model.addAttribute("page", pageArg);
        model.addAttribute("size", sizeArg);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("sort", sortArg);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalElements", pageResult.getTotalElements());
        model.addAttribute("currentUserId", userId);
        
        // 加入當前使用者的顯示名稱
        String currentUserDisplayName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("訪客");
        model.addAttribute("currentUserDisplayName", currentUserDisplayName);
        
        return "listings";
    }
    
    // M2: 我的刊登頁面（只顯示自己的刊登）
    @GetMapping("/my-listings")
    public String myListings(@RequestParam(required = false) Integer page,
                             @RequestParam(required = false) Integer size,
                             @RequestParam(required = false) String q,
                             @RequestParam(required = false) String sort,
                             HttpSession session,
                             Model model) {
        // 如果未登入，重定向到登入頁
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }

        // 預設值（與後端 API 對齊：page 1 起算、size 預設 6）
        Integer pageArg = (page == null || page <= 0) ? 1 : page;
        Integer sizeArg = (size == null || size <= 0) ? 6 : Math.min(size, 100);
        String sortArg = (sort == null || sort.isBlank()) ? "createdAt,DESC" : sort;

        Long userId = (Long) session.getAttribute("userId");
        // 只顯示當前使用者的刊登
        Page<ListingDTO> pageResult = listingService.myListingsPage(userId, pageArg, sizeArg, q, sortArg);

        model.addAttribute("items", pageResult.getContent());
        model.addAttribute("page", pageArg);
        model.addAttribute("size", sizeArg);
        model.addAttribute("q", q == null ? "" : q);
        model.addAttribute("sort", sortArg);
        model.addAttribute("totalPages", pageResult.getTotalPages());
        model.addAttribute("totalElements", pageResult.getTotalElements());
        model.addAttribute("currentUserId", userId);
        
        // 加入當前使用者的顯示名稱
        String currentUserDisplayName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("訪客");
        model.addAttribute("currentUserDisplayName", currentUserDisplayName);
        
        return "my-listings";
    }

    // M3: 新增刊登的專屬頁面
    @GetMapping("/listings/new")
    public String newListingForm(HttpSession session, Model model) {
        // 如果未登入，重定向到登入頁
        if (session.getAttribute("userId") == null) {
            return "redirect:/ui/auth/login";
        }

        Long userId = (Long) session.getAttribute("userId");
        // 加入當前使用者的顯示名稱
        String currentUserDisplayName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("訪客");
        model.addAttribute("currentUserDisplayName", currentUserDisplayName);
        model.addAttribute("currentUserId", userId);

        return "create-listing";
    }
    
    // 保留其他物品相關頁面和 API
    
    // === 圖片上傳 API (for listings) ===
    // 注意：此端點路徑為 /ui/api/images/upload，供刊登物品頁面使用
    // 全局的 /api/images/upload 在 ImageUploadController 中，供聊天室使用
    
    @PostMapping("/api/images/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).build();
        }
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 檢查檔案
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "請選擇圖片檔案");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 檢查檔案大小 (50MB)
            if (file.getSize() > 50 * 1024 * 1024) {
                response.put("success", false);
                response.put("message", "圖片檔案大小不可超過 50MB");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 檢查檔案類型
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "請上傳圖片檔案");
                return ResponseEntity.badRequest().body(response);
            }
            
            // 生成檔名
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;
            
            // 建立上傳目錄
            Path uploadDir = Paths.get("uploads/images");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            
            // 儲存檔案
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);
            
            response.put("success", true);
            response.put("fileName", fileName);
            response.put("url", "/images/" + fileName);
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "檔案上傳失敗：" + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
