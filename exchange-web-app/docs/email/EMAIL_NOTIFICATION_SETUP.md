# 電子郵件通知系統使用說明

## 系統概述

本系統實現了全面的 HTML 格式電子郵件通知功能，採用專業的品牌設計，讓用戶可以清楚識別這是來自「卡片交換平台」的官方郵件，而不是個人電子郵件。

## 🎨 郵件設計特色

### 視覺設計
- **品牌識別**：醒目的紫色漸層標題（#7c3aed - #6d28d9）
- **圖標系統**：每種通知類型都有對應的 emoji 圖標
- **響應式設計**：支援各種裝置和郵件客戶端
- **專業排版**：使用 HTML 表格確保跨平台兼容性

### 郵件元素
1. **Header 區域**
   - 大型圖標（48px）
   - 平台名稱「卡片交換平台」（白色 28px 粗體）
   - 英文標識「Exchange Platform」

2. **Content 區域**
   - 清晰的標題和內容分隔
   - 重要資訊使用粗體和顏色強調
   - 直達平台的按鈕（紫色漸層）

3. **Footer 區域**
   - 系統提示（請勿回覆）
   - 發送時間
   - 版權資訊

## 📧 郵件發送者設定

郵件發送者名稱已設定為「卡片交換平台」，用戶收到郵件時會顯示：
```
寄件人：卡片交換平台 <aacindy1026@gmail.com>
```

配置位置：`application.yml`
```yaml
spring:
  mail:
    from-name: 卡片交換平台
    from-email: aacindy1026@gmail.com
```

## 📋 通知類型與設計

### 提案相關通知
- **📨 PROPOSAL_RECEIVED** - 收到新提案
  - 圖標：📨
  - 強調：提案編號
  - 行動呼籲：查看並回應提案

- **✅ PROPOSAL_ACCEPTED** - 提案被接受
  - 圖標：✅
  - 內容：恭喜訊息
  - 下一步：協商配送方式

- **❌ PROPOSAL_REJECTED** - 提案被拒絕
  - 圖標：❌
  - 內容：鼓勵用戶繼續嘗試

### 交換流程相關通知
- **🔄 SWAP_CONFIRMED** - 交換確認
- **📋 DELIVERY_METHOD_PROPOSED** - 運送方式提案
- **✅ DELIVERY_METHOD_ACCEPTED** - 運送方式確認

### 物流相關通知
- **📦 SHIPMENT_SENT** - 包裹已寄出
  - 包含：追蹤號碼（如有）
- **📬 SHIPMENT_RECEIVED** - 包裹已送達

### 完成相關通知
- **🎉 EXCHANGE_COMPLETED** - 交換完成
  - 邀請：留下評價

### 🔐 驗證碼郵件

#### 註冊驗證
- **圖標**：🎉
- **標題**：歡迎加入卡片交換平台！
- **驗證碼顯示**：大型紫色漸層按鈕樣式（32px，字母間距 8px）
- **有效時間**：10分鐘（紅色強調）

#### 郵箱變更驗證
- **圖標**：🔐
- **標題**：電子郵件變更驗證
- **驗證碼顯示**：同註冊樣式
- **安全提示**：如非本人操作請聯繫客服

## 💻 技術實現

### HTML 郵件支援
- 使用 `MimeMessageHelper` 發送 HTML 郵件
- 設定 `setText(content, true)` 啟用 HTML 格式
- 完整的 HTML5 DOCTYPE 和 meta 標籤

### 郵件模板架構
```java
// 主要通知郵件
generateHtmlTemplate(icon, title, message, entityId)

// 驗證碼郵件
generateVerificationEmailTemplate(icon, title, message)
```

### 異步發送
所有郵件都使用 `@Async` 異步發送，不會阻塞主要業務流程。

## 🔧 使用方式

### 發送通知郵件
```java
// 提案通知
emailNotificationService.sendProposalNotification(proposal, 
    NotificationType.PROPOSAL_RECEIVED, 
    receiverId);

// 交換通知
emailNotificationService.sendSwapNotification(swap, 
    NotificationType.DELIVERY_METHOD_PROPOSED, 
    recipientId);

// 物流通知
emailNotificationService.sendShipmentNotification(shipment, 
    NotificationType.SHIPMENT_SENT, 
    recipientId, 
    trackingNumber);
```

### 發送驗證碼
```java
// 註冊驗證
emailNotificationService.sendVerificationCode(
    email, 
    verificationCode, 
    "REGISTER"
);

// 郵箱變更驗證
emailNotificationService.sendVerificationCode(
    newEmail, 
    verificationCode, 
    "CHANGE_EMAIL"
);
```

## 🎯 設計亮點

1. **品牌一致性**
   - 所有郵件使用統一的紫色漸層主題
   - 與網站 UI 設計風格一致

2. **清晰的視覺層次**
   - Header：品牌識別
   - Content：重要資訊
   - Footer：系統提示

3. **強調重點資訊**
   - 使用紫色高亮重要文字
   - 編號用 `#` 標示更醒目
   - 驗證碼採用大型按鈕樣式

4. **優秀的可讀性**
   - 使用微軟正黑體等中文友善字體
   - 適當的行距（line-height: 1.8）
   - 清晰的段落分隔

5. **行動呼籲按鈕**
   - 紫色漸層背景
   - 陰影效果增加立體感
   - 清楚的文字「🔗 前往平台查看」

## 📱 跨平台兼容性

郵件使用 HTML 表格布局，確保在以下環境正常顯示：
- Gmail（網頁版、App）
- Outlook（Windows、Mac、網頁版）
- Apple Mail（macOS、iOS）
- Yahoo Mail
- 其他主流郵件客戶端

## 🚀 測試建議

1. **發送測試郵件**：
```bash
curl -X POST "http://localhost:8080/api/notifications/test?type=PROPOSAL_RECEIVED" \
  -H "Cookie: JSESSIONID=your_session_id"
```

2. **檢查郵件外觀**：
   - 在不同郵件客戶端打開
   - 確認圖標、顏色、排版正確
   - 測試按鈕連結

3. **驗證碼測試**：
   - 註冊新帳號
   - 變更電子郵件
   - 確認驗證碼顯示清楚

## 📊 改進效果

相比之前的純文字郵件：
- ✅ 更專業的品牌形象
- ✅ 更高的用戶信任度
- ✅ 更清晰的資訊層次
- ✅ 更好的閱讀體驗
- ✅ 更高的點擊率（行動呼籲按鈕）