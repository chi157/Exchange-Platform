# 資料庫設定指南

## 1. 建立 MySQL 資料庫

在執行應用程式之前，請先在 MySQL 中建立資料庫：

```sql
CREATE DATABASE exchange_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 2. 設定資料庫連線

編輯 `src/main/resources/application.yml` 檔案，修改以下設定：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/exchange_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Taipei
    username: your_mysql_username  # 修改為你的 MySQL 使用者名稱
    password: your_mysql_password  # 修改為你的 MySQL 密碼
```

## 3. Flyway 自動建立資料表

啟動應用程式時，Flyway 會自動執行 `src/main/resources/db/migration/` 目錄下的 SQL 腳本，建立所有資料表：

- V1: users 資料表
- V2: listings 資料表
- V3: proposals 與 proposal_items 資料表
- V4: swaps 資料表
- V5: shipments 與 shipment_events 資料表
- V6: messages 資料表
- V7: reviews 與 disputes 資料表

## 4. 預設管理員帳號

系統會自動建立一個管理員帳號：

- **Email**: admin@exchange-platform.com
- **Password**: admin123
- **Roles**: ROLE_USER, ROLE_ADMIN

## 5. 驗證資料庫連線

啟動應用程式後，檢查 console 輸出，確認：
- Flyway migrations 成功執行
- JPA 成功連線資料庫
- 沒有出現連線錯誤

## 6. 常見問題

### 問題：連線失敗
- 確認 MySQL 服務正在執行
- 確認使用者名稱和密碼正確
- 確認資料庫 `exchange_db` 已建立

### 問題：Flyway migration 失敗
- 檢查 SQL 語法是否正確
- 確認資料表不存在衝突
- 查看 `flyway_schema_history` 資料表了解執行狀態

### 問題：時區設定
- 確保 MySQL 設定了正確的時區
- URL 中已包含 `serverTimezone=Asia/Taipei` 參數
