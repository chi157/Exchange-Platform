## UC-14 / 自動交換配對
Title: Automatic Matching

優先級: Could

參與者: User, System (Matching Engine)

前置條件:
- 系統有多筆 ACTIVE 的 listings，且使用者開啟自動配對選項。

觸發條件:
- 使用者選擇「自動配對」或系統定期批次執行 match job。

主要流程:
1. 使用者設定欲交換條件（類別、版本、品況範圍、地區、物流偏好）。
2. Matching engine 計算符合的 listings 並評分（基於條件相符度、距離、信譽等）。
3. 若某組合達到閾值，系統建立 Proposal 草案或直接送出通知給雙方，需雙方確認後方可建立 Swap。

例外 / 替代流程:
- 複雜多方配對（multi-to-one 或連鎖交換）需轉為人工審核或提示使用者手動確認。
- 若匹配的物品在建立 proposal 之前被鎖定，系統跳過該候選並嘗試下一候選。

後置條件 / 產出:
- 系統生成 Proposal 建議並通知相關使用者；若接受則進入 Swap 流程。

關聯 UI / API:
- POST `/matches/auto`, GET `/matches/suggestions`, POST `/proposals`（基於建議）

相關資料 / 實體:
- matches(job_id, user_id, candidate_listings[], score, created_at)

可度量的驗收標準（Gherkin）:
1. Given User 設定自動配對條件
   When 系統找到一組相符度高於閾值的候選
   Then 系統建立 Proposal 草案並通知雙方

2. Given 配對候選在 proposal 建立前被鎖定
   When 系統嘗試建立 proposal
   Then 系統跳過該候選並向使用者顯示替代建議

3. Given 系統自動建立提案草案
   When 雙方其中一方接受草案
   Then 系統變更 Proposal.status 為 ACCEPTED_BY_B 並等待另一方確認

備註:
- Matching engine 可先以簡單規則實作（閾值比對），再逐步引入機器學習提升配對品質。
