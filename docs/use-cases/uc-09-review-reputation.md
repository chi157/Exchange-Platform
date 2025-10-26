## UC-09 / 評價與信譽
Title: Review and Reputation

優先級: Must

參與者: User (交易雙方), Admin

前置條件:
- Swap 已完成（COMPLETED）。

觸發條件:
- 交易完成後系統通知雙方進行評價。

主要流程:
1. 交易完成後，系統在 Swap 頁提供評價表單，包含分項評分（誠信、包裝、出貨速度、品況準確）與留言欄位。
2. 使用者提交評價，系統儲存 Review 並更新被評者的 reputationScore（例如加權平均）。
3. 評價後系統更新公開/私密顯示（視產品策略）並統計展示於使用者頁。

例外 / 替代流程:
- 若評價含違規言論或私人資訊，Admin 可審核並刪除不當評價。
- 若雙方未評價，系統可在期限後發出催促通知或自動標記為未評價。

後置條件 / 產出:
- reviews 資料表新增紀錄，被評者的 reputationScore 更新。

關聯 UI / API:
- POST `/swaps/{id}/review`，GET `/users/{id}/reviews`

相關資料 / 實體:
- reviews(id, swap_id, reviewer_id, reviewed_user_id, scores, comment, created_at)

可度量的驗收標準（Gherkin）:
1. Given Swap 為 COMPLETED
   When A 提交針對 B 的評分（包含四項分數）
   Then 系統儲存 review 並更新 B 的 reputationScore

2. Given 一則 review 被判定含有違規內容
   When Admin 檢視並刪除該 review
   Then 該 review 從公開顯示移除並更新被評者的分數

3. Given 使用者在期限內未評價
   When 評價期限過
   Then 系統發送催促通知並在日後統計為未評價

備註:
- reputationScore 的計算法應保留歷史計算方式以便回溯並支援權重調整。
