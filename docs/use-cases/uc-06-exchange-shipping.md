## UC-06 / 交換成立與出貨
Title: Exchange Confirmation and Shipping

優先級: Must

參與者: Trader A, Trader B, Shipping Carrier

前置條件:
- Swap 已建立（雙方互相確認）。

觸發條件:
- Swap 狀態為 INIT，雙方需在出貨期限內出貨。

主要流程:
1. 系統在 Swap 頁面顯示出貨期限與建議物流方式。
2. 任一方打包並出貨後，上傳託運單或輸入 carrier + tracking number 及上傳運單圖檔（POST `/swaps/{id}/shipment`）。
3. 系統儲存 shipment 記錄並嘗試向 carrier 查詢或等待 webhook 更新。
4. 系統通知對方已出貨，並在追蹤事件更新時提醒。

例外 / 替代流程:
- 任一方未在出貨期限內出貨 → 另一方可取消交易並留下紀錄，系統記錄違約並可能影響信譽。
- 上傳運單無效 → 系統提示錯誤並請重新上傳/由 Admin 稽核。

後置條件 / 產出:
- Shipment 記錄被創建與追蹤資訊更新；Swap 進入運送階段並等待 Delivery/確認。

關聯 UI / API:
- POST `/swaps/{id}/shipment`，GET `/swaps/{id}`（顯示 shipment 狀態），carrier webhook `/integrations/carrier/{carrier}/webhook`

相關資料 / 實體:
- shipments(id, swap_id, sender_id, carrier_name, tracking_number, shipped_at, events)

可度量的驗收標準（Gherkin）:
1. Given Swap 狀態為 INIT 且 A 準備出貨
   When A 上傳合法的 tracking number
   Then 系統儲存 shipment 並顯示「已出貨」狀態給雙方

2. Given A 未在 ship_deadline 內出貨
   When deadline 過期
   Then 系統通知 B 並允許 B 取消交易與留下違規紀錄

3. Given A 上傳 tracking number
   When carrier webhook 傳回狀態更新
   Then 系統更新 shipment.events 並在 Swap 頁顯示最新狀態

備註:
- 若 carrier 不支援 webhook，建議使用 scheduler 進行外部 poll。
