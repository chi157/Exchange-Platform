# Exchange Platform — MVP 規格（最小可行功能）

本文件描述系統在課堂/作業用的最小功能集（MVP），聚焦完成度與可驗證性，省略所有安全強化與非必要功能。

## In Scope（本階段一定做）

1) 認證（已完成）
- 明碼密碼 + HttpSession（無加密、無 HTTPS、無信件驗證）
- API：
  - POST /api/auth/register
  - POST /api/auth/login
  - POST /api/auth/logout
  - GET  /api/auth/me

2) Listings（刊登）
- 最小欄位：id, title, description, ownerId, createdAt, updatedAt
- API：
  - GET /api/listings/{id}
  - GET /api/listings?page=&size=&q=&sort （頁碼從 1 開始；size 預設 10；q 可搜尋 title/description；sort=屬性[,ASC|DESC]，允許屬性：id, createdAt, updatedAt）
  - GET /api/listings/{id}/proposals?page=&size=&sort（列出該 listing 的 proposals）
  - POST /api/listings（需登入）

3) Proposals（交換提案）
- 最小欄位：id, listingId, proposerId, message, status(PENDING/ACCEPTED/REJECTED), createdAt
- API：
  - POST /api/proposals（需登入）
  - POST /api/proposals/{id}/accept（listing 擁有者）
  - POST /api/proposals/{id}/reject（listing 擁有者）
  - GET  /api/proposals/mine?page=&size=&sort（列出我提出的 proposals；需登入）
  - GET  /api/proposals/received?page=&size=&sort（列出我收到的 proposals，即我擁有之 listings 的 proposals；需登入）

## Out of Scope（MVP 不納入）

- 安全性強化（OAuth/JWT/加密/CSRF/權限）
- 信件/簡訊/2FA
- 即時聊天/WebSocket
- 物流/寄送/追蹤
- 評價/黑名單/風險
- 主題市集/自動配對/收藏追蹤
- 金流/支付/退款
- 系統監控/Actuator

## 技術原則（MVP）

- 後端：Spring Boot 3（Web, Data JPA, Validation）
- DB：執行用 MySQL；測試用 H2（`src/test/resources/application-test.yml`）
- Schema：`spring.jpa.hibernate.ddl-auto=update`（避免引入 Flyway 複雜度）
- Session：HttpSession（Cookie JSESSIONID）
- 測試：
  - Web 層單元測試（@WebMvcTest + Mock Service）
  - 整合測試（@SpringBootTest + H2 + MockMvc），每條主流程至少 1 個 happy path + 1 個未授權/驗證失敗情境

## 開發/測試指南

### 執行（本機）

```powershell
# 方式 A：使用本機 MySQL（application.yml）
cd "E:\NCU\1141\Software Engineering\Exchange-Platform\exchange-web-app"
mvn -DskipTests spring-boot:run

# 方式 B：使用 H2（測試用設定），啟動時套用 test profile（快速本機驗證）
cd "E:\NCU\1141\Software Engineering\Exchange-Platform\exchange-web-app"
mvn -DskipTests -Dspring-boot.run.profiles=test spring-boot:run
```

### 測試（H2）

```powershell
cd "E:\NCU\1141\Software Engineering\Exchange-Platform\exchange-web-app"
mvn test
```

### Postman 手測（範例）

1) 註冊：POST /api/auth/register
```json
{
  "email": "user@test.com",
  "password": "pw123",
  "displayName": "User"
}
```

2) 登入：POST /api/auth/login（啟用 Cookie Jar 保留 JSESSIONID）
```json
{
  "email": "user@test.com",
  "password": "pw123"
}
```

3) 取得目前使用者：GET /api/auth/me

4) 登出：POST /api/auth/logout

5) 建立 Listing（需登入）：POST /api/listings
```json
{
  "title": "My Item",
  "description": "Simple desc"
}
```

6) 查詢 Listing：GET /api/listings?page=1&size=10&q=&sort=createdAt,DESC

7) 取得單筆：GET /api/listings/{id}

8) 列出我的提案：GET /api/proposals/mine?page=1&size=10

9) 列出我收到的提案：GET /api/proposals/received?page=1&size=10

10) 列出某個刊登的提案：GET /api/listings/{id}/proposals?page=1&size=10

---

此文件會在每次功能增減時同步更新，以維持專案目標清晰與可驗證性。