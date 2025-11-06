package com.exchange.platform.controller;

import com.exchange.platform.service.TrackingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tracking")
@RequiredArgsConstructor
public class TrackingController {

    private final TrackingService trackingService;

    /**
     * 取得驗證碼圖片
     * @return 驗證碼圖片的 session ID
     */
    @PostMapping("/get-captcha")
    public ResponseEntity<Map<String, String>> getCaptcha(HttpSession httpSession) {
        try {
            String sessionId = trackingService.generateCaptcha(httpSession);
            return ResponseEntity.ok(Map.of("sessionId", sessionId));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 取得驗證碼圖片檔案
     */
    @GetMapping("/captcha-image")
    public ResponseEntity<Resource> getCaptchaImage(@RequestParam String sessionId) {
        try {
            Resource image = trackingService.getCaptchaImage(sessionId);
            if (image != null && image.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                        .body(image);
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 使用驗證碼查詢物流狀態
     * @param sessionId 驗證碼 session ID
     * @param code 使用者輸入的驗證碼
     * @param trackingNumber 追蹤號碼
     */
    @PostMapping("/query")
    public ResponseEntity<?> queryTracking(@RequestParam String sessionId,
                                           @RequestParam String code,
                                           @RequestParam String trackingNumber) {
        try {
            Map<String, Object> result = trackingService.queryTracking(sessionId, code, trackingNumber);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
