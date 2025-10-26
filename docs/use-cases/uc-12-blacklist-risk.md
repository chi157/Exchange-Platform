## UC-12 / 黑名單與風險警示
Title: Blacklist and Risk Alerts

優先級: Should

參與者: Admin, System

前置條件:
- 系統有使用者行為紀錄（未出貨、仲裁敗訴、被檢舉等）。

觸發條件:
- 系統自動偵測風險條件或 Admin 根據檢舉決議加入黑名單。

主要流程:
1. 系統依規則（多次未出貨、濫訴、詐欺行為）標記使用者風險等級或直接限制帳號功能。
2. Admin 可手動將使用者加入黑名單或解除限制，並記錄處理理由。
3. 系統在 UI 顯示風險提示，並限制該帳號進行上架或發提案等特定操作。

例外 / 替代流程:
- 使用者提出申訴要求復審 → Admin 依證據復核並可恢復權限。

後置條件 / 產出:
- user.status 或 risk_flags 更新；相關交易權限被調整。

關聯 UI / API:
- Admin: GET `/admin/users/{id}`, POST `/admin/users/{id}/ban`, POST `/admin/users/{id}/unban`

相關資料 / 實體:
- users(risk_score, banned_until, flags), admin_actions(log)

可度量的驗收標準（Gherkin）:
1. Given 使用者有 3 次未出貨紀錄
   When 系統風控規則觸發
   Then 系統將該使用者標示為高風險並限制發提案權限

2. Given Admin 將使用者封禁
   When 執行封禁操作
   Then 該使用者無法上架或發起新提案，且 admin_actions 留下紀錄

3. Given 使用者申訴
   When Admin 完成復審並決定解除封禁
   Then 系統恢復該使用者的操作權限並記錄變更

備註:
- 風控策略應保留可調整的閾值與白名單機制，以避免誤判。
