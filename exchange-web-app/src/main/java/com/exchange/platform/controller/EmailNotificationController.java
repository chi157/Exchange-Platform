package com.exchange.platform.controller;

import com.exchange.platform.entity.EmailNotification;
import com.exchange.platform.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class EmailNotificationController {

    private final EmailNotificationService emailNotificationService;
    private static final String SESSION_USER_ID = "userId";

    /**
     * 重試失敗的通知（管理員功能）
     */
    @PostMapping("/retry-failed")
    public ResponseEntity<Map<String, String>> retryFailedNotifications(HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登入"));
        }
        
        // 這裡可以加入管理員權限檢查
        // 暫時允許所有登入用戶執行（實際應用中應該限制管理員）
        
        emailNotificationService.retryFailedNotifications();
        return ResponseEntity.ok(Map.of("message", "已重新發送失敗的通知"));
    }

    /**
     * 測試發送通知（開發/測試用途）
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, String>> testNotification(
            @RequestParam String type,
            @RequestParam(required = false) Long targetUserId,
            HttpSession session) {
        
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "未登入"));
        }
        
        Long recipientId = targetUserId != null ? targetUserId : userId;
        
        try {
            EmailNotification.NotificationType notificationType = 
                    EmailNotification.NotificationType.valueOf(type.toUpperCase());
            
            emailNotificationService.createAndSendNotification(
                    recipientId, 
                    notificationType, 
                    "Test", 
                    999L, 
                    "測試參數"
            );
            
            return ResponseEntity.ok(Map.of("message", "測試通知已發送"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "無效的通知類型"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "發送失敗: " + e.getMessage()));
        }
    }
}