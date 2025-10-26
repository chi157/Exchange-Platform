## UC-05 / 交換協商（站內聊）
Title: Negotiation (In-app Chat)

優先級: Must

參與者: Trader A, Trader B

前置條件:
- Proposal 已建立且雙方能存取該提案頁面。

觸發條件:
- 任一方於 Proposal 頁面或 Swap 頁面發送訊息或修改提案內容。

主要流程:
1. 使用者於提案頁面發送文字或附檔訊息，系統將訊息儲存至 messages table，並透過 WebSocket 或 push 通知對方。
2. 若一方修改提案（加入/移除物品、調整到貨期限），系統檢查變更是否合法並更新 Proposal；若涉及新增物品需再次鎖定該物品。
3. 雙方達成共識時，任一或雙方按「同意」，當雙方都同意時，系統建立 Swap（ExchangeRecord）。

例外 / 替代流程:
- 若修改導致鎖定衝突，系統回傳錯誤並要求使用者調整。
- 任一方取消提案 → Proposal 及其鎖定釋放。

後置條件 / 產出:
- 若雙方同意：建立 Swap 並通知雙方；否則 proposal 繼續在協商中或已取消。 

關聯 UI / API:
- WebSocket endpoint `/ws/proposals/{id}`，POST `/proposals/{id}/update`，GET `/proposals/{id}/messages`

相關資料 / 實體:
- messages(id, proposal_id or swap_id, from_user_id, content, attachments, created_at)

可度量的驗收標準（Gherkin）:
1. Given Proposal 頁面開啟
   When A 發送一則訊息給 B
   Then 訊息存入資料庫並即時推播給 B

2. Given A 在協商中新增一件自己要交換的 item
   When 新項目尚可上鎖
   Then 系統將該 item 鎖定並更新 Proposal

3. Given 雙方在聊天後達成協議
   When 雙方皆按下「同意」
   Then 系統建立 Swap 並進入出貨階段

備註:
- 建議使用 STOMP over WebSocket 並搭配 Redis 作為 broker 以便水平擴充。
