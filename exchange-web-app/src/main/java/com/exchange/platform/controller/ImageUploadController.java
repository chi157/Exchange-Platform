package com.exchange.platform.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 圖片上傳控制器
 * 提供統一的圖片上傳 API
 */
@Controller
public class ImageUploadController {

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
