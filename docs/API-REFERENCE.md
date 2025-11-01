# Exchange Platform API 參考與測試指南（c04）

本文件說明每支 API 的用途、請求與回應格式、常見狀態碼，以及如何以 Postman 或 PowerShell 進行測試。系統為 Session-based 登入（JSESSIONID Cookie）。對外的分頁頁碼 page 一律「1 起算」。

- 服務位址（預設）：http://localhost:8080
- 登入機制：成功登入後，伺服器以 Set-Cookie 回傳 `JSESSIONID`，後續請求需帶上該 Cookie 才視為已登入
- Profiles：
  - default（MySQL，見 `application.yml`）
  - test（H2，見 `src/test/resources/application-test.yml`）

---

## 認證 Auth

### POST /api/auth/register
- 用途：註冊新使用者
- 請求（JSON）：
```json
{
  "email": "user@test.com",
  "password": "pw123",
  "displayName": "User"
}
```
- 回應（201 Created）：
```json
{
  "success": true,
  "message": "Registration successful",
  "userId": 1,
  "email": "user@test.com",
  "displayName": "User"
}
```
- 錯誤：400（驗證失敗）

測試（PowerShell，可貼到終端機執行）：
```powershell
# 註冊
curl -Method POST "http://localhost:8080/api/auth/register" -ContentType "application/json" -Body '{"email":"u@test.com","password":"pw","displayName":"U"}'
```

### POST /api/auth/login
- 用途：登入並建立 Session
- 請求（JSON）：
```json
{
  "email": "user@test.com",
  "password": "pw123"
}
```
- 回應（200 OK 或 401 Unauthorized）：
```json
{
  "success": true,
  "message": "Login successful"
}
```
- 備註：成功時會在回應標頭 `Set-Cookie` 帶回 `JSESSIONID`

測試：
```powershell
# 登入，並觀察回應標頭的 Set-Cookie: JSESSIONID
curl -Method POST "http://localhost:8080/api/auth/login" -ContentType "application/json" -Body '{"email":"u@test.com","password":"pw"}' -i
```

### GET /api/auth/me
- 用途：取得目前登入使用者資訊
- 回應（200 或 401）：
```json
{
  "id": 1,
  "email": "u@test.com",
  "displayName": "U",
  "verified": false,
  "roles": "USER"
}
```
測試（需帶 Cookie）：
```powershell
# 將 <cookie> 替換成實際 JSESSIONID，例如 "JSESSIONID=ABC..."（可從登入回應取得）
curl -Method GET "http://localhost:8080/api/auth/me" -Headers @{"Cookie"="<cookie>"}
```

### POST /api/auth/logout
- 用途：登出（銷毀 Session）
- 回應：204 No Content

---

## 刊登 Listings

DTO（回應）概形：
```json
{
  "id": 1,
  "title": "My Item",
  "description": "Desc",
  "ownerId": 7,
  "createdAt": "2025-11-02T02:00:00",
  "updatedAt": "2025-11-02T02:00:00"
}
```

### POST /api/listings
- 用途：建立刊登（需登入）
- 請求（JSON）：
```json
{ "title": "T", "description": "D" }
```
- 回應：201 Created + ListingDTO
- 錯誤：401 未登入

測試：
```powershell
curl -Method POST "http://localhost:8080/api/listings" -Headers @{"Cookie"="<cookie>"} -ContentType "application/json" -Body '{"title":"T","description":"D"}'
```

### GET /api/listings/{id}
- 用途：取得單一刊登
- 回應：200 OK + ListingDTO；404 Not Found

測試：
```powershell
curl -Method GET "http://localhost:8080/api/listings/1"
```

### GET /api/listings
- 用途：查詢列表（支援分頁/搜尋/排序）
- 參數：
  - `page`（可選，1 起算，預設 1）
  - `size`（可選，預設 10，上限 100）
  - `q`（可選，搜尋 title/description）
  - `sort`（可選，格式：`欄位[,ASC|DESC]`；允許欄位：`id`, `createdAt`, `updatedAt`；預設 `createdAt,DESC`）
- 回應：200 OK + ListingDTO[]（目前回傳陣列，尚未封裝分頁中繼資料）

測試：
```powershell
curl -Method GET "http://localhost:8080/api/listings?page=1&size=10&q=&sort=createdAt,DESC"
```

### GET /api/listings/{id}/proposals
- 用途：列出某個刊登的所有提案（MVP：不需登入）
- 參數：`page`, `size`, `sort`（同上，1-based 分頁；白名單排序）
- 回應：200 OK + ProposalDTO[]（可能為空陣列）

測試：
```powershell
curl -Method GET "http://localhost:8080/api/listings/1/proposals?page=1&size=10&sort=createdAt,DESC"
```

---

## 提案 Proposals

DTO（回應）概形：
```json
{
  "id": 10,
  "listingId": 1,
  "proposerId": 2,
  "message": "hi",
  "status": "PENDING",
  "createdAt": "2025-11-02T02:00:00",
  "updatedAt": "2025-11-02T02:00:00"
}
```

### POST /api/proposals
- 用途：對某個刊登提出交換提案（需登入）
- 請求（JSON）：
```json
{ "listingId": 1, "message": "offer" }
```
- 回應：201 Created + ProposalDTO（初始 `status=PENDING`）
- 錯誤：
  - 401 未登入
  - 404 listing 不存在

測試：
```powershell
curl -Method POST "http://localhost:8080/api/proposals" -Headers @{"Cookie"="<cookie>"} -ContentType "application/json" -Body '{"listingId":1,"message":"offer"}'
```

### POST /api/proposals/{id}/accept
- 用途：刊登擁有者接受提案（需登入）
- 回應：200 OK + ProposalDTO（`status=ACCEPTED`）
- 錯誤：
  - 401 未登入
  - 403 非刊登擁有者
  - 404 提案或刊登不存在

測試：
```powershell
curl -Method POST "http://localhost:8080/api/proposals/10/accept" -Headers @{"Cookie"="<cookie>"}
```

### POST /api/proposals/{id}/reject
- 用途：刊登擁有者拒絕提案（需登入）
- 回應：200 OK + ProposalDTO（`status=REJECTED`）
- 錯誤：同 accept

測試：
```powershell
curl -Method POST "http://localhost:8080/api/proposals/10/reject" -Headers @{"Cookie"="<cookie>"}
```

### GET /api/proposals/mine
- 用途：列出我提出的提案（需登入）
- 參數：`page`, `size`, `sort`（1-based；白名單排序）
- 回應：200 OK + ProposalDTO[]
- 錯誤：401 未登入

測試：
```powershell
curl -Method GET "http://localhost:8080/api/proposals/mine?page=1&size=10&sort=createdAt,DESC" -Headers @{"Cookie"="<cookie>"}
```

### GET /api/proposals/received
- 用途：列出我「收到」的提案（即我擁有之刊登的所有提案，需登入）
- 參數：同上
- 回應：200 OK + ProposalDTO[]
- 錯誤：401 未登入

測試：
```powershell
curl -Method GET "http://localhost:8080/api/proposals/received?page=1&size=10" -Headers @{"Cookie"="<cookie>"}
```

---

## 交換 Swaps（M3）

DTO（回應）概形：
```json
{
  "id": 1,
  "listingId": 7,
  "proposalId": 101,
  "aUserId": 1,
  "bUserId": 2,
  "status": "IN_PROGRESS",
  "aConfirmedAt": null,
  "bConfirmedAt": null,
  "createdAt": "2025-11-02T02:00:00",
  "updatedAt": "2025-11-02T02:00:00",
  "completedAt": null
}
```

建立時機：當刊登擁有者「接受」一筆提案時，系統自動建立 Swap，並將該 Listing 標記為 LOCKED，避免重複受理。

### GET /api/swaps/mine
- 用途：列出我參與的所有 Swaps（包含我當 A 或 B）
- 參數：`page`, `size`, `sort`（1-based；允許欄位：id, createdAt, updatedAt）
- 回應：200 OK + SwapDTO[]
- 錯誤：401 未登入

測試：
```powershell
curl -Method GET "http://localhost:8080/api/swaps/mine?page=1&size=10" -Headers @{"Cookie"="<cookie>"}
```

### GET /api/swaps/{id}
- 用途：取得單筆 Swap 詳情（限參與者）
- 回應：200 OK + SwapDTO；403 非參與者；404 不存在；401 未登入

測試：
```powershell
curl -Method GET "http://localhost:8080/api/swaps/1" -Headers @{"Cookie"="<cookie>"}
```

重點行為：
- 重複接受同一刊登上的其它提案 → 409 Conflict（因該 Listing 已被鎖定 LOCKED）

---

## 收貨確認（M5）

雙方各自呼叫確認收貨；當兩方都確認後，Swap 會標記為 `COMPLETED` 並填入 `completedAt`。

### POST /api/swaps/{id}/confirm-received
- 用途：由「目前登入使用者」（限該 Swap 參與者）確認已收貨
- 回應：200 OK + SwapDTO（具備 `aConfirmedAt`/`bConfirmedAt` 欄位與最新 `status`）
- 錯誤：
  - 401 未登入
  - 403 非該 Swap 參與者
  - 404 Swap 不存在
- 行為：
  - 冪等：同一使用者重複呼叫不會出錯，狀態維持不變
  - 當雙方皆確認後，`status` 變為 `COMPLETED`，並填入 `completedAt`

測試（PowerShell）：
```powershell
curl -Method POST "http://localhost:8080/api/swaps/1/confirm-received" -Headers @{"Cookie"="<cookie>"}
```

備註：
- 自動逾時完成（timeout auto-complete）可於後續擴充（目前未內建排程）；現階段以雙方主動確認為準。

---

## 狀態碼與邊界規則彙整
- 2xx：成功（201 Created 用於新增）
- 400：參數或驗證錯誤（例如註冊輸入不合法）
- 401：未登入（需 Session 的端點）
- 403：沒有權限（例如非刊登擁有者嘗試 accept/reject）
- 404：資源不存在（提案或刊登不存在）

分頁/排序規則：
- `page`：1 起算；`page <= 1` 視為第 1 頁
- `size`：預設 10；`size <= 0` 視為 10；上限 100
- `sort`：`欄位[,ASC|DESC]`；允許欄位為 `id`, `createdAt`, `updatedAt`，其他欄位一律回退 `createdAt,DESC`

---

## 本機啟動與測試

- 使用 MySQL 啟動（預設 profile）：
```powershell
cd "E:\NCU\1141\Software Engineering\Exchange-Platform\exchange-web-app"
mvn -DskipTests spring-boot:run
```
- 使用 H2 啟動（test profile，免安裝 DB）：
```powershell
cd "E:\NCU\1141\Software Engineering\Exchange-Platform\exchange-web-app"
$env:SPRING_PROFILES_ACTIVE='test'; mvn -DskipTests spring-boot:run
```

若你偏好，我可以再加一個 `application-dev.yml`（H2）供「開發」使用，與測試 profile 分離。

---

以上內容對齊現有控制器與測試（截至 2025-11-02，branch: feature/mvp2）。若未來 API 形狀變動（例如加入分頁中繼資料），本檔會同步更新。

---

## 出貨 Shipments（M4）

每個 Swap 的雙方各自最多擁有「一筆」出貨資料（唯一約束：同一 `swapId` + `senderId` 只會有一筆）。只有出貨方（sender）本人可以建立/更新自己的出貨資料，並且只有 sender 本人可以對該出貨新增事件。

ShipmentDTO（回應）概形：
```json
{
  "id": 10,
  "swapId": 1,
  "senderId": 2,
  "deliveryMethod": "SHIPNOW",
  "trackingNumber": "1122-3344-5566",
  "trackingUrl": "https://track.example/1122-3344-5566",
  "lastStatus": "PACKED",
  "shippedAt": null,
  "createdAt": "2025-11-02T02:00:00",
  "updatedAt": "2025-11-02T02:00:00"
}
```

支援的 deliveryMethod 值：
- 請求值（不分大小寫）："shipnow" 或 "face_to_face"
- 回應枚舉："SHIPNOW" 或 "FACE_TO_FACE"

### POST /api/swaps/{id}/shipments/my
- 用途：建立或更新「我」在該 Swap 的出貨資訊（需登入、需為該 Swap 參與者）
- 請求（JSON）：
```json
{
  "deliveryMethod": "shipnow",
  "trackingNumber": "1122-3344-5566",
  "trackingUrl": "https://track.example/1122-3344-5566"
}
```
- 回應：200 OK + ShipmentDTO（首次建立亦回 200）
- 錯誤：
  - 401 未登入
  - 403 非該 Swap 參與者
  - 404 Swap 不存在
  - 400 deliveryMethod 非法（僅允許 shipnow 或 face_to_face）

測試（PowerShell）：
```powershell
curl -Method POST "http://localhost:8080/api/swaps/1/shipments/my" -Headers @{"Cookie"="<cookie>"} -ContentType "application/json" -Body '{"deliveryMethod":"shipnow","trackingNumber":"1122-3344-5566","trackingUrl":"https://track.example/1122-3344-5566"}'
```

### POST /api/shipments/{id}/events
- 用途：在指定 Shipment 新增事件（需登入、僅 sender 本人可新增）
- 請求（JSON）：
```json
{
  "status": "PACKED",
  "note": "已妥善包裝",
  "at": "2025-11-02T12:34:56"
}
```
- 回應：201 Created（無 body）
- 錯誤：
- 401 未登入
- 403 非該 Shipment 的 sender
- 404 Shipment 不存在
- 400 參數錯誤（status 為必填非空，at 為必填時間）

測試（PowerShell）：
```powershell
curl -Method POST "http://localhost:8080/api/shipments/10/events" -Headers @{"Cookie"="<cookie>"} -ContentType "application/json" -Body '{"status":"PACKED","note":"已妥善包裝","at":"2025-11-02T12:34:56"}' -i
```

備註：
- 事件新增後，系統會同步更新 Shipment.lastStatus = 請求中的 status。
- 目前未提供查詢事件列表與查詢「我的」Shipment 的 GET 端點；如有需要可在後續 M4.1 擴充：
  - GET /api/swaps/{id}/shipments/my（回傳我在該 Swap 的 ShipmentDTO）
  - GET /api/shipments/{id}/events（回傳 ShipmentEventDTO[]）