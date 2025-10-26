## UC-07 / 物流追蹤
Title: Shipment Tracking

優先級: Must

參與者: Trader A (寄件人/收件人), Trader B (寄件人/收件人), Admin

前置條件:
- Shipment 已建立。系統僅允許兩種交付方式：`賣貨便`（平台指定的物流服務）或 `face_to_face`（面交）。
- 若交付方式為 `賣貨便`，寄件人必須在建立或更新 Shipment 時提供有效的物流編號（tracking number）；系統不會自動從運輸商拉取狀態，但會在 UI 顯示可點擊的查詢連結供對方查詢。

觸發條件:
- 每位寄件人（Trader A、Trader B）在 UI 或透過 API 主動設定或更新該方所寄出的 Shipment（包含選擇交付方式與填寫物流編號），或在運送過程中手動新增 shipment event。

主要流程:
1. 每位寄件人（Trader A 與 Trader B）各自登入系統並在自己的寄件頁面或交換明細中，手動新增該方所寄出 Shipment 的 event（例如：PICKED_UP、IN_TRANSIT、DELIVERED）、選擇交付方式（`賣貨便` 或 `face_to_face`），或標記為「面交」。
   - 若選擇 `賣貨便`，寄件人須輸入物流編號（tracking number）。系統會儲存該編號，並在 UI 提供一個可點擊的查詢連結（例如：外部 carrier 查詢 URL 或平台查詢頁面），供對方查詢運單狀態。
2. 系統驗證請求者為該 Shipment 的擁有者（owner/寄件人），僅允許擁有者更新該筆 Shipment。系統將 event 存入 `shipment.events`，更新 `shipment.last_status` 與 `updated_at`，並根據狀態更新對應的 Swap 狀態（例如：IN_TRANSIT、DELIVERED）。
3. 系統向交易雙方發送通知（例如：App 推播、Email）以告知最新狀態；若為 `賣貨便`，通知中包含「查詢物流」的連結。
4. 若寄件人標記為 `face_to_face`（面交），系統將該 Shipment 的追蹤需求設為 optional/disabled，並允許雙方在完成面交後手動確認交付。

例外 / 替代流程:
- 若寄件人未在預期時間內更新狀態：系統將該 Shipment 標示為 PENDING 或 UNKNOWN，並在 UI 提示另一方聯絡對方或由 Admin 稽核。
- 若任一方嘗試偽造或重複提交錯誤狀態：系統應驗證身分並可要求上傳補充證明（例如：交付照片、簽收截圖、GPS 時戳）。

後置條件 / 產出:
- Shipment.events、shipment.last_status 與 Swap 狀態被正確記錄並保留歷史事件；若為面交，可能不會有 carrier 事件紀錄。

關聯 UI / API:
- POST `/shipments/{id}/events`  (各寄件人手動新增其所屬 Shipment 事件，系統需驗證為該 Shipment 的擁有者)
- PATCH `/shipments/{id}` (更新交付方式為 `賣貨便` 或 `face_to_face`、設定/更新 `tracking_number`)
- GET `/shipments/{tracking}/status`, GET `/swaps/{id}`
- UI: Shipment 詳情頁顯示 `tracking_number`（若有）與「查詢物流」連結；對方可點擊連結查詢運單狀態。

相關資料 / 實體:
- shipments(tracking_number, events[], last_status, delivery_method{賣貨便|face_to_face}, tracking_url?, updated_at)

可度量的驗收標準（Gherkin）:
1. Given Trader A 已發出物品並登入系統
   When Trader A 選擇交付方式為 `賣貨便` 並輸入有效的 tracking number
   Then 系統儲存 `tracking_number`、顯示並在通知中包含「查詢物流」連結，且 Trader B 可點擊查詢該運單狀態

2. Given Trader B 已發出物品並登入系統
   When Trader B 選擇交付方式為 `賣貨便` 並輸入有效的 tracking number
   Then 系統儲存 `tracking_number`、顯示並在通知中包含「查詢物流」連結，且 Trader A 可點擊查詢該運單狀態

3. Given 交易選擇面交 (face-to-face)
   When 任一寄件人將交付方式標記為 `face_to_face`
   Then 系統不要求 carrier 狀態，並允許雙方在完成面交後手動確認交付

4. Given 任一寄件人未更新狀態且超過期待交付時間
   When 系統檢查到逾時
   Then 系統在 UI 顯示 PENDING/UNKNOWN 並提示聯絡對方或交由 Admin 稽核

備註:
- 由於狀態由寄件人手動維護，API 與 UI 應加強寄件人驗證流程，並提供上傳交付證明的選項以降低冒用或爭議風險。
