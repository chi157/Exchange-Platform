## UC-10 / 爭議申訴與仲裁
Title: Dispute and Arbitration

優先級: Must

參與者: Claimant (申訴方), Respondent (被申訴方), Admin

前置條件:
- Swap 未完成或完成後仍在可申訴時效內。

觸發條件:
- 任一方在 Swap 頁點擊「申訴」並上傳證據（照片/影片/聊天記錄）。

主要流程:
1. 申訴方填寫申訴原因並上傳證據，系統建立 Dispute 紀錄並通知 Admin 與對方。
2. Admin 檢視雙方證據、聊天紀錄與運單，必要時要求補件。
3. Admin 在時限內裁定結果（例如：同意退回、重新交換、駁回），系統根據裁定更新 Swap 狀態與使用者信譽。

例外 / 替代流程:
- 證據不足 → Admin 請雙方補件或暫時凍結 Swap。 
- 若發現惡意濫訴或偽造證據 → Admin 可對濫訴方進行警告或封禁處理。

後置條件 / 產出:
- Dispute 記錄與裁定結果存檔；依裁定更新 Swap 與使用者權限/信譽。

關聯 UI / API:
- POST `/swaps/{id}/dispute`，GET `/admin/disputes`，POST `/admin/disputes/{id}/resolve`

相關資料 / 實體:
- disputes(id, swap_id, claimant_id, reason, evidence_refs, status, admin_resolution, created_at)

可度量的驗收標準（Gherkin）:
1. Given 收貨者上傳「品況不符」證據
   When 申訴提交
   Then 系統建立 Dispute 並通知 Admin 與對方

2. Given Admin 收到完整雙方證據
   When Admin 在 48 小時內做初審
   Then Admin 可記錄初審意見並在 7 天內完成結案

3. Given Admin 裁定退回
   When 系統執行裁定
   Then Swap 狀態更新並通知雙方，且被裁定方信譽分依策略調整

4. Given 證據被判定為偽造
   When Admin 確認偽造事證
   Then 系統對偽造方採取警告或封禁措施並記錄事件

備註:
- 建議保存上傳檔案之原始 hash 與 metadata 以便查驗與保全證據。
