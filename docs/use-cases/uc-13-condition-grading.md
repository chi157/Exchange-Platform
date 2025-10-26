## UC-13 / 品況分級與參考指南
Title: Condition Grading and Reference Guide

優先級: Should

參與者: User, Admin

前置條件:
- 上架表單需要選擇品況等級。

觸發條件:
- 使用者在上架或查看 listing 時需要參考品況分級。

主要流程:
1. 系統提供品況分級規範（S/A/B/C），含文字說明與範例照片/範例情境。 
2. 上架者於上架表單選擇品況並填寫缺陷說明。
3. Admin 可依分級標準進行抽樣稽核，若與照片或描述不符則要求修改或拒絕上架。

例外 / 替代流程:
- 若上架者填寫與圖片不符，Admin 可標示為需修改並拒絕上架；上架者可修改後重新送審。

後置條件 / 產出:
- Listing 附帶 condition 欄位，稽核結果影響 listing 是否上架。

關聯 UI / API:
- GET `/help/condition-guide`, 顯示於上架頁面作為輔助說明

相關資料 / 實體:
- listings.condition, admin_review_notes

可度量的驗收標準（Gherkin）:
1. Given 使用者在上架頁面
   When 查看品況說明
   Then 系統顯示每個等級的文字說明與範例圖片

2. Given 上架者選擇「A」但圖片顯示嚴重缺陷
   When Admin 審核該 listing
   Then Admin 可以拒登並要求修改，listing.status 變為 PENDING_MODIFICATION

3. Given Admin 稽核後要求修改
   When 上架者完成修改並重新提交
   Then 系統重新送至待審列表

備註:
- 建議將品況說明放在上架 UI 的 tooltip 或幫助區塊，減少誤判與爭議發生。
