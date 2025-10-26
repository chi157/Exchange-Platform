## UC-08 / 收貨檢查與完成
Title: Delivery Confirmation and Completion

優先級: Must

參與者: Recipient (收貨者), Sender, Admin

前置條件:
- Shipment 顯示為 DELIVERED（或系統收到使用者手動標記為已收到）。

觸發條件:
- 收貨者在 Swap 頁面點選「確認收貨且品況無誤」或提出申訴。

主要流程:
1. 收貨者檢查收到物品，若品況無誤，點選「完成」並可上傳收貨照片/影片作為紀錄。
2. 系統更新 Swap 的收貨方確認狀態（例如 RECEIVED_B_CONFIRMED）。
3. 若雙方皆確認，系統標記 Swap 為 COMPLETED 並啟動評價流程。
4. 若收貨者未在設定時間內確認，系統依設定自動完成交易（可配置緩衝時間）。

例外 / 替代流程:
- 若品況不符：收貨者提出申訴，進入 UC-10 爭議流程。
- 若一方惡意拒絕確認：系統記錄並通知 Admin 稽核。

後置條件 / 產出:
- Swap 更新為 COMPLETED（若雙方確認或自動完成）；評價介面開放。

關聯 UI / API:
- POST `/swaps/{id}/confirm_received`，POST `/swaps/{id}/dispute`

相關資料 / 實體:
- swaps(status, received_a_confirmed, received_b_confirmed, completed_at)

可度量的驗收標準（Gherkin）:
1. Given 收貨者在到貨 3 日內確認物況無誤
   When 收貨者按下「完成」
   Then 系統將 Swap 標記為 COMPLETED 並開啟評價

2. Given 收貨者上傳照片並選擇「品況不符」
   When 收貨者提交申訴
   Then 系統建立 Dispute 並通知 Admin 與對方

3. Given 收貨者超過 72 小時未確認
   When 自動完成時限到期
   Then 系統自動標記 Swap 為 COMPLETED（若無爭議）並通知雙方

備註:
- 自動完成的時限與條件應可配置，且在自動完成前再次通知使用者。
