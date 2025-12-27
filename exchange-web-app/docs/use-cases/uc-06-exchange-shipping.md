## UC-06 / 交換成立與出貨
Title: Exchange Confirmation and Shipping

優先級: Must

參與者: Trader A (寄件人/收件人), Trader B (寄件人/收件人), Admin

前置條件:
- Swap 已建立（雙方互相確認）。

觸發條件:
- Swap 狀態為 INIT，雙方需在出貨期限內出貨。

主要流程:
1. 系統在 Swap 頁面顯示出貨期限與平台支援的交付方式（僅限 `交貨便` 或 `face_to_face`）。
2. 任一方打包並出貨後，上傳託運單或在建立/更新 Shipment 時選擇交付方式並輸入必要資訊：
   - 若選擇 `交貨便`，必須輸入 `tracking_number`（POST `/swaps/{id}/shipment`），系統會儲存該運單資訊並在 Shipment 詳情頁顯示「查詢物流」連結；系統不會自動向運輸商查詢狀態。
   - 若選擇 `face_to_face`（面交），則可標記為面交，系統不要求提供 tracking number，並允許雙方在面交完成後手動確認交付。
3. 系統通知對方已出貨，通知中若有 `tracking_number` 則包含可點擊的查詢連結；交換雙方可在 Swap/Shipment 詳情查看狀態與歷史事件。

例外 / 替代流程:
- 任一方未在出貨期限內出貨 → 另一方可取消交易並留下紀錄，系統記錄違約並可能影響信譽。
- 上傳運單無效或缺少 `tracking_number`（當選擇 `交貨便` 時）→ 系統提示錯誤並請重新上傳/由 Admin 稽核。

後置條件 / 產出:
- Shipment 記錄被創建並儲存 `tracking_number`（若為交貨便）或標記為 `face_to_face`；Swap 進入運送階段並等待寄件人/收件人手動更新交付狀態或確認交付。

關聯 UI / API:
- POST `/swaps/{id}/shipment`（建立或更新 Shipment，包含 `delivery_method` 與 `tracking_number`）
- GET `/swaps/{id}`（顯示 shipment 狀態）
- UI: Shipment 詳情頁顯示 `tracking_number`（若有）與「查詢物流」連結；若為 `交貨便`，對方可點擊查詢運單狀態。

相關資料 / 實體:
- shipments(id, swap_id, sender_id, delivery_method{交貨便|face_to_face}, tracking_number?, tracking_url?, shipped_at, events)

可度量的驗收標準（Gherkin）:
1. Given Swap 狀態為 INIT 且 A 準備出貨
   When A 選擇 `交貨便` 並上傳合法的 tracking number
   Then 系統儲存 shipment（包含 `tracking_number`）並在 Swap/Shipment 詳情顯示「已出貨」與查詢連結給雙方

2. Given A 未在 ship_deadline 內出貨
   When deadline 過期
   Then 系統通知 B 並允許 B 取消交易與留下違規紀錄

3. Given A 選擇 `face_to_face`
   When A 標記為面交且雙方在同意後確認交付
   Then 系統標記該 Shipment 為已交付並更新 Swap 狀態

備註:
- 本平台不主動向運輸商拉取狀態；若使用 `交貨便`，寄件人需提供 `tracking_number`，系統會提供查詢連結供對方查看外部運單狀態。
