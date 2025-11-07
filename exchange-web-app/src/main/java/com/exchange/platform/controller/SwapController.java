package com.exchange.platform.controller;

import com.exchange.platform.dto.SwapDTO;
import com.exchange.platform.service.SwapService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/swaps")
@RequiredArgsConstructor
public class SwapController {

    private final SwapService swapService;

    @GetMapping("/mine")
    public ResponseEntity<List<SwapDTO>> listMine(@RequestParam(required = false) Integer page,
                                                  @RequestParam(required = false) Integer size,
                                                  @RequestParam(required = false) String sort,
                                                  HttpSession session) {
        return ResponseEntity.ok(swapService.listMine(session, page, size, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SwapDTO> getById(@PathVariable Long id, HttpSession session) {
        return ResponseEntity.ok(swapService.getById(id, session));
    }

    @PostMapping("/{id}/confirm-received")
    public ResponseEntity<SwapDTO> confirmReceived(@PathVariable Long id, HttpSession session) {
        return ResponseEntity.ok(swapService.confirmReceived(id, session));
    }

    @PostMapping("/{id}/meetup")
    public ResponseEntity<?> setMeetupInfo(@PathVariable Long id, 
                                           @RequestBody Map<String, String> payload,
                                           HttpSession session) {
        try {
            String location = payload.get("location");
            String timeStr = payload.get("time");
            String notes = payload.get("notes");
            
            if (location == null || location.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "地點不能為空"));
            }
            if (timeStr == null || timeStr.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "時間不能為空"));
            }
            
            // 嘗試解析多種日期格式
            LocalDateTime time;
            try {
                // 嘗試 ISO_DATE_TIME (2024-11-07T14:30:00.000Z)
                if (timeStr.contains("Z") || timeStr.contains("+")) {
                    time = LocalDateTime.parse(timeStr.substring(0, 19));
                } else {
                    // ISO_LOCAL_DATE_TIME (2024-11-07T14:30:00 or 2024-11-07T14:30)
                    time = LocalDateTime.parse(timeStr);
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "時間格式錯誤"));
            }
            
            SwapDTO result = swapService.setMeetupInfo(id, location, time, notes, session);
            return ResponseEntity.ok(result);
        } catch (SwapService.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未授權"));
        } catch (SwapService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "無權限"));
        } catch (SwapService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "交換不存在"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "儲存失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/meetup/confirm")
    public ResponseEntity<?> confirmMeetup(@PathVariable Long id, HttpSession session) {
        try {
            SwapDTO result = swapService.confirmMeetup(id, session);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SwapService.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未授權"));
        } catch (SwapService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "無權限"));
        } catch (SwapService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "交換不存在"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "確認失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/delivery-method/propose")
    public ResponseEntity<?> proposeDeliveryMethod(@PathVariable Long id,
                                                   @RequestBody Map<String, String> payload,
                                                   HttpSession session) {
        try {
            String method = payload.get("method");
            if (method == null || method.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "配送方式不能為空"));
            }
            
            SwapDTO result = swapService.proposeDeliveryMethod(id, method, session);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SwapService.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未授權"));
        } catch (SwapService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "無權限"));
        } catch (SwapService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "交換不存在"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "提議失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/delivery-method/confirm")
    public ResponseEntity<?> confirmDeliveryMethod(@PathVariable Long id, HttpSession session) {
        try {
            SwapDTO result = swapService.confirmDeliveryMethod(id, session);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (SwapService.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未授權"));
        } catch (SwapService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "無權限"));
        } catch (SwapService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "交換不存在"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "確認失敗: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/delivery-method/reject")
    public ResponseEntity<?> rejectDeliveryMethod(@PathVariable Long id, HttpSession session) {
        try {
            SwapDTO result = swapService.rejectDeliveryMethod(id, session);
            return ResponseEntity.ok(result);
        } catch (SwapService.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "未授權"));
        } catch (SwapService.ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "無權限"));
        } catch (SwapService.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "交換不存在"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "拒絕失敗: " + e.getMessage()));
        }
    }

    @ExceptionHandler(SwapService.UnauthorizedException.class)
    public ResponseEntity<Void> handleUnauthorized() { return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); }

    @ExceptionHandler(SwapService.ForbiddenException.class)
    public ResponseEntity<Void> handleForbidden() { return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); }

    @ExceptionHandler(SwapService.NotFoundException.class)
    public ResponseEntity<Void> handleNotFound() { return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); }
}
