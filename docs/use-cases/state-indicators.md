## 系統狀態指標總表

目標：集中說明系統中與出貨/交換相關的所有狀態、事件名稱、可接受值、欄位規則與簡短更新權限說明，供開發與文件一致參考使用。

一、實體對應
- Shipment：單一寄件紀錄（單方向，owner 為寄件人）
- Swap：交換交易（包含雙方各自的 Shipment）
- Event / shipment.events：一筆 shipment 的歷史事件紀錄

二、交付方式（delivery_method）
- 賣貨便 ("maimai_logistics" / 標記為 `賣貨便`)：平台指定物流；若選擇此方式，寄件人必須提供 `tracking_number`。
- 面交 ("face_to_face")：雙方面交，不需要運單號（tracking_number 可為 null）。

三、Shipment 欄位（關鍵欄）
- id
- swap_id
- owner_id (寄件人)
- delivery_method {賣貨便 | face_to_face}
- tracking_number (required 當 delivery_method == 賣貨便)
- tracking_url (可選，由系統組成或由寄件人/平台提供)
- last_status (enum)
- events [] (歷史事件陣列)
- shipped_at, updated_at

四、Shipment.last_status（允許值）
- CREATED：寄件紀錄已建立（尚未出貨）
- PICKED_UP：物流已取件（寄件人或物流標記）
- IN_TRANSIT：運送中
- OUT_FOR_DELIVERY：派送中
- DELIVERED：已送達 / 已交付
- RETURNED：退回
- FAILED_ATTEMPT：投遞失敗（例如無人簽收）
- DELAYED：延遲
- CANCELLED：寄件取消
- UNKNOWN：狀態不明或未更新（逾時提示時使用）
- LOST / DAMAGED：運送途中遺失或損壞（視需要紀錄）

註：系統以 `last_status` 顯示目前狀態，所有變化應以 event 的方式加入 `events[]`，以保留完整歷史。

五、shipment.events（事件格式）
- 建議欄位：{ event_id, status, actor_id, actor_role, timestamp, metadata }
  - status 範例值同上（PICKED_UP、IN_TRANSIT、DELIVERED 等）
  - actor_id：執行更新的使用者 id（通常為寄件人或系統/管理者）
  - actor_role：'sender' | 'admin' | 'system'
  - metadata：可包含照片 URL、外部證明、GPS 時戳或留言

六、Swap 狀態（高層交易狀態）
- INIT：交換建立，尚未出貨
- AWAITING_SHIPMENT：等待出貨（寄件人尚未上傳/標記）
- IN_TRANSIT：至少一方的 Shipment 處於運送中（或被標記為已出貨）
- AWAITING_CONFIRMATION：所有寄件方均已標記已出貨／完成，等待對方確認收貨（或雙方面交確認）
- COMPLETED：雙方確認交付，交換完成
- CANCELLED：交易被取消
- DISPUTE：產生爭議，需人工介入

七、權限與更新規則
- Shipment 只允許該 Shipment 的 owner（寄件人）或 Admin 更新其狀態與 events（例如：上傳交付證據）。
- 轉換規則建議：
  - CREATED -> PICKED_UP -> IN_TRANSIT -> OUT_FOR_DELIVERY -> DELIVERED
  - 任一時點可被標記為 CANCELLED / DELAYED / FAILED_ATTEMPT / RETURNED
  - face_to_face 流程：CREATED -> (face_to_face 標記) -> AWAITING_CONFIRMATION -> DELIVERED/COMPLETED

八、驗證規則（API 層）
- 當 `delivery_method == 賣貨便`：PATCH/POST 建立或更新 shipment 必須包含 `tracking_number`（非空）。
- `tracking_url` 若由系統產生，格式應為有效 URL；可透過 `tracking_number` 與平台運送查詢 URL 模板組成。
- 當新增 event 時，系統應檢查 actor 是否為 owner 或 admin，並記錄 timestamp 與來源 IP（如需）。

九、通知與 UI 行為
- 當 owner 新增或更新 event 並改變 `last_status` 時，系統應通知對方（推播/Email）；若 event 帶有 `tracking_number`，通知內應包含可點擊的「查詢物流」連結。
- UI 顯示：Shipment 卡片上顯示 `delivery_method`、`tracking_number`（如有）與 `last_status`；點擊 tracking number 或「查詢物流」會開啟 `tracking_url`。

十、逾時/例外處理
- 若在預期交付期間未見 owner 更新狀態，系統應標示 `UNKNOWN` 或 `PENDING`，並在 UI 顯示提示，或自動建立提醒任務給 owner 與 admin。
- 若 event 含疑義（例：被重複或偽造），系統可要求上傳補充證據（照片、簽收截圖、GPS 時戳），或標示為需人工稽核狀態（DISPUTE 或需 Admin 確認）。

十一、範例 API 片段（簡短）
- 建立/更新 Shipment（POST / PATCH）：
  {
    "delivery_method": "賣貨便",
    "tracking_number": "AB123456789",
    "tracking_url": "https://tracking.example.com/AB123456789"
  }
- 新增 Shipment event（POST `/shipments/{id}/events`）：
  {
    "status": "DELIVERED",
    "actor_id": 123,
    "metadata": { "photo_url": "https://..." }
  }

十二、備註
- 若日後欲改為自動與運輸商整合（webhook / polling），可把 `tracking_number` 作為關鍵值以串接外部 API，但目前設計為以寄件人手動維護為主。
- 文件維護：此檔案為單一來源（source of truth）用來對齊開發、前端與產品行為，任何狀態值或流程變更請同步更新此檔。

© Documentation generated for Exchange-Platform use-cases
