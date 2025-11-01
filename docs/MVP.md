# 交換平台 MVP 說明

> 本文件定義本專案最小可行產品（MVP）的功能範圍、介面、資料模型、測試與執行方式，聚焦「能跑起來、可基本交易互動」；安全性暫不納入考量（依照需求：明碼儲存、HTTP 傳送密碼可接受）。

## 目標與範圍

- 目標：提供使用者註冊/登入，發布物品（Listing），以及對他人物品提出提案（Proposal），並讓物品擁有者接受或拒絕提案。
- 範圍（In scope）
	- 帳號註冊／登入／登出／查詢自己資訊（Session-based）
	- 物品（Listing）建立、單筆查詢、列表
	- 提案（Proposal）建立、接受、拒絕
- 非範圍（Out of scope / 先不做）
	- 密碼雜湊、OAuth、CSRF、CORS 等進階安全性
	- 通知、聊天室、物流、評價、黑名單、搜尋過濾、主題市集等延伸功能
	- 後台管理、權限階層、風險引擎

## 技術棧與架構

- 後端：Spring Boot 3.5.x（Web、Validation、Spring Data JPA）
- ORM：Hibernate 6.6.x
- DB（執行時）：MySQL 8
- DB（測試）：H2 in-memory（MySQL 相容模式）
- 測試：JUnit 5、MockMvc
- 組態：`src/main/resources/application.yml`（JPA `ddl-auto: update`）
- 認證：純 Session（`HttpSession`，Session key: `userId`），密碼明碼儲存（MVP）

## 資料模型（重點欄位）

- User（簡化）
	- id, email, passwordHash（實際存明碼）、displayName
	- roles（VARCHAR，非 JSON）、riskScore（int）、isBlacklisted（boolean）
	- createdAt, updatedAt
- Listing
	- id, title, description, ownerId
	- 既有資料庫相容：
		- `ownerId` 對應欄位 `user_id`
		- 另映射 `owner_id`（舊欄位）為 `ownerIdLegacy`，在 `@PrePersist` 同步
- Proposal
	- id, listingId, proposerId, message, status（PENDING/ACCEPTED/REJECTED）
	- 既有資料庫相容：
		- `proposee_listing_id`（舊欄位）：在 `@PrePersist` 以 `listingId` 補上
		- `receiver_id`（舊欄位）：建立時以該 listing 的擁有者 `ownerId` 補上

> 註：上述 Legacy 欄位為滿足既有 MySQL schema 的 NOT NULL 約束，不影響 API 介面。

## API 一覽

- 基底路徑：`/api`

### Auth
- POST `/api/auth/register`
	- Body: `{ "email":"a@test.com", "password":"pw", "displayName":"A" }`
	- 201 Created，回傳使用者基本資訊
- POST `/api/auth/login`
	- Body: `{ "email":"a@test.com", "password":"pw" }`
	- 200 OK，建立 Session（Cookie）
- GET `/api/auth/me`
	- 200 OK 回傳自己資訊；未登入 401
- POST `/api/auth/logout`
	- 200 OK，銷毀 Session

### Listings
- POST `/api/listings`（需登入）
	- Body: `{ "title":"My Item", "description":"Desc" }`
	- 201 Created，回傳 `{ id, title, description, ownerId, createdAt, updatedAt }`
- GET `/api/listings/{id}`
	- 200 OK（存在）／404 Not Found（不存在）
- GET `/api/listings`
	- 200 OK，回傳列表（MVP 未加分頁）

### Proposals
- POST `/api/proposals`（需登入）
	- Body: `{ "listingId": 123, "message": "offer" }`
	- 201 Created，回傳 `{ id, listingId, proposerId, message, status, createdAt, updatedAt }`
	- 可能錯誤：401 未登入、404 listing 不存在
- POST `/api/proposals/{id}/accept`（需為該 listing 擁有者）
	- 200 OK，`status` 變為 `ACCEPTED`
	- 可能錯誤：401 未登入、403 非擁有者、404 提案或 listing 不存在
- POST `/api/proposals/{id}/reject`（需為該 listing 擁有者）
	- 200 OK，`status` 變為 `REJECTED`
	- 可能錯誤：同上

## 典型流程（Postman 示範）

A 發佈物品 → B 對該物品提案 → A 接受或拒絕

1) A 註冊並登入（取得 A 的 Session）
2) A 建立 Listing：
	 - POST `/api/listings` → 取得回應中的 `id`（例如 123）
3) 以 B 的帳號登入（切成 B 的 Session，可用另一個請求標籤頁）
4) B 建立 Proposal：
	 - POST `/api/proposals`，Body: `{ "listingId": 123, "message": "offer" }`
5) A 接受 / 拒絕該 Proposal：
	 - POST `/api/proposals/{proposalId}/accept` 或 `/reject`

排錯提示：
- 送 Proposal 前，用 `GET /api/auth/me` 確認是 B 的身分；若仍是 A，清除 Cookies 或使用另一個 Postman 空間分頁。
- 伺服器重啟後，Session 會失效，需要重新登入。

## 執行方式（Windows / PowerShell）

前置需求：JDK 17、Maven、MySQL 8、建立資料庫 `exchange_db`
- 請依實際安裝調整 `exchange-web-app/src/main/resources/application.yml` 內的 `spring.datasource.*`

啟動（跳過測試）：

```powershell
cd "E:\NCU\1141\Software Engineering\Exchange-Platform\exchange-web-app"
mvn -DskipTests spring-boot:run
```

或打包並以 Jar 執行：

```powershell
mvn -DskipTests package
java -jar .\target\exchange-web-app-*.jar
```

## 測試方式

- 單元／Web 層／整合測試皆備，測試使用 H2（MySQL 模式）與 profile `test`

執行全部測試：

```powershell
cd "E:\NCU\1141\Software Engineering\Exchange-Platform\exchange-web-app"
mvn test
```

涵蓋情境（節選）：
- Auth：註冊 → 登入 → me → 登出 → 未登入 me → 401
- Listings：登入建立 → 取得 → 列表 → 未登入建立 401
- Proposals：A 建立 Listing → B 提案 → B 接受 403 → A 接受 200 → A 拒絕 200 → 未登入建立 401 → 接受不存在 404

## 既有資料庫相容策略

- User：`roles` 以 VARCHAR 儲存（避免 JSON 型別問題）
- Listing：
	- 主欄位 `ownerId` 對應 DB `user_id`
	- 舊欄位 `owner_id` 以 `ownerIdLegacy` 映射，`@PrePersist` 同步
- Proposal：
	- 舊欄位 `proposee_listing_id` 在 `@PrePersist` 以 `listingId` 補上
	- 舊欄位 `receiver_id` 在建立時以 `listing.ownerId` 補上

如再遇到其他 NOT NULL 舊欄位，採相同策略於 Entity 映射並於保存時補值。

## 已知限制與風險

- 密碼明碼儲存、HTTP 明文傳輸（僅為 MVP 快速驗證）
- 未做權限模型、速率限制、輸入清理、防重複提案等
- 未導入 Flyway；以 `ddl-auto: update` 動態對齊欄位（僅限開發期）

## 後續路線圖（建議）

1. Proposal 查詢 API（依 Listing 或 proposer 分頁查詢）
2. 權限與安全（密碼雜湊、HTTPS、CORS/CSRF、JWT 或強化 Session 管理）
3. 資料庫遷移（Flyway）並清理 Legacy 欄位
4. Listing 分頁與搜尋/關鍵字過濾
5. 通知／聊天室／物流等進階功能

## 附錄：常見錯誤與對應

- 401 Unauthorized：未登入或 Session 遺失
- 403 Forbidden：非 Listing 擁有者嘗試接受/拒絕提案
- 404 Not Found：資源不存在（Listing 或 Proposal）
- 500 內部錯誤（常見於舊欄位 NOT NULL 約束）：
	- 檢查是否已重啟後端載入最新 Entity 映射
	- 回報錯誤訊息（欄位名），按相容策略在 Entity 補映射與預設值
