package com.exchange.platform.controller;

import com.exchange.platform.service.EmailService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/test/email")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    /**
     * 測試郵件發送 API
     * GET /api/test/email/send?to=your-email@example.com
     */
    @GetMapping("/send")
    public ResponseEntity<Map<String, Object>> sendTestEmail(
            @RequestParam String to,
            HttpSession session) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // 驗證是否登入
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("success", false);
                response.put("message", "請先登入");
                return ResponseEntity.status(401).body(response);
            }
            
            // 發送測試郵件
            emailService.sendTestEmail(to);
            
            response.put("success", true);
            response.put("message", "測試郵件已發送至: " + to);
            response.put("to", to);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "郵件發送失敗: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
