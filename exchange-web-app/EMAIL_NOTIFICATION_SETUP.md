# 電子郵件通知系統使用說明

## 系統概述

本系統實現了全面的電子郵件通知功能，當平台上發生重要事件時，會自動發送電子郵件通知給相關用戶。

## 已實現的通知類型

### 提案相關通知
- `PROPOSAL_RECEIVED` - 收到新提案
- `PROPOSAL_ACCEPTED` - 提案被接受  
- `PROPOSAL_REJECTED` - 提案被拒絕
- `PROPOSAL_WITHDRAWN` - 提案被撤回

### 交換流程相關通知
- `SWAP_CONFIRMED` - 交換確認
- `DELIVERY_METHOD_PROPOSED` - 運送方式提案
- `DELIVERY_METHOD_ACCEPTED` - 運送方式確認
- `DELIVERY_METHOD_REJECTED` - 運送方式被拒絕
- `MEETUP_SCHEDULED` - 面交時間確認

### 物流相關通知
- `SHIPMENT_SENT` - 包裹已寄出
- `SHIPMENT_RECEIVED` - 包裹已送達
- `TRACKING_UPDATE` - 物流狀態更新

### 完成相關通知
- `EXCHANGE_COMPLETED` - 交換完成
- `REVIEW_REMINDER` - 評價提醒

### 系統相關通知
- `DISPUTE_CREATED` - 爭議處理開始
- `ACCOUNT_VERIFICATION` - 帳號驗證
- `SECURITY_ALERT` - 安全提醒

## 資料庫設置

1. 執行 SQL 腳本創建 email_notifications 表：
```bash
mysql -u root -p exchange_db < create-email-notifications-table.sql
```

2. 確認表已成功創建：
```sql
DESCRIBE email_notifications;
```

## 郵件配置

確認 `application.yml` 中的郵件配置正確：

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: aacindy1026@gmail.com
    password: ephc ehzw ufur zhon
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

**注意**: 如需使用不同的郵件服務商，請修改 host、port 等配置。

## 已整合的觸發點

### ProposalService
- 創建提案時 → 發送 `PROPOSAL_RECEIVED` 給接收者
- 接受提案時 → 發送 `PROPOSAL_ACCEPTED` 給提案者

### SwapService  
- 提議運送方式時 → 發送 `DELIVERY_METHOD_PROPOSED` 給對方
- 雙方確認運送方式時 → 發送 `DELIVERY_METHOD_ACCEPTED` 給雙方

### ShipmentService
- 填入追蹤號碼（首次寄出）時 → 發送 `SHIPMENT_SENT` 給收件人

## API 端點

### 重試失敗通知（管理功能）
```http
POST /api/notifications/retry-failed
```

### 測試通知（開發用）
```http
POST /api/notifications/test?type=PROPOSAL_RECEIVED&targetUserId=1
```

## 防重複機制

系統自動防止重複發送相同通知：
- 5分鐘內相同類型和實體的通知不會重複發送
- 使用 `EmailNotificationRepository.findRecentNotificationsByTypeAndEntity()` 檢查

## 異步處理

電子郵件發送使用 `@Async` 異步處理，不會阻塞主要業務流程。

## 錯誤處理

- 發送失敗的通知保持 `sent=false` 狀態
- 可通過 `retryFailedNotifications()` 方法重新發送
- 日誌記錄所有發送狀態和錯誤信息

## 通知內容

每個通知類型都有對應的：
- 主題模板（根據類型生成）
- 內容模板（包含相關編號和操作指引）
- 平台連結（導向到 `app.base-url` 配置）

## 下一步擴展

可以進一步擴展的功能：
1. HTML 郵件模板（目前是純文字）
2. 用戶郵件偏好設置（選擇接收的通知類型）
3. 郵件模板管理介面
4. 通知歷史查詢 API
5. 更多觸發點（評價提醒、交換完成等）

## 測試建議

1. 確認郵件配置正確：
```bash
curl -X POST "http://localhost:8080/api/notifications/test?type=PROPOSAL_RECEIVED" \
  -H "Cookie: JSESSIONID=your_session_id"
```

2. 查看日誌確認發送狀態：
```bash
tail -f logs/exchange-web-app.log | grep "電子郵件通知"
```

3. 檢查資料庫記錄：
```sql
SELECT * FROM email_notifications ORDER BY created_at DESC LIMIT 10;
```