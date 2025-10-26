## UC-04 / 發起交換提案
Title: Create Proposal (Initiate Swap Proposal)

優先級: Must

參與者: Trader A (Proposer), Trader B (Listing Owner)

前置條件:
- Proposer 與 Listing Owner 為已登入 User。
- 被提案之 Listing 為 ACTIVE 且未被鎖定（locked_by_proposal_id is null）。

觸發條件:
- Proposer 在 Listing 詳情頁點選「發起交換」。

主要流程:
1. Proposer 選擇自己願意交換的一或多件 listing 並填寫提案訊息、物流偏好與到貨期限（選填）。
2. 系統於 DB transaction 中檢查 Proposer 選取物是否為 ACTIVE 且未鎖定；若可用，建立 Proposal 並為該物件設定 locked_by_proposal_id。Proposal 設定 expiresAt（TTL）。
3. 系統產生提案摘要並通知 Listing Owner（站內通知 + email）。
4. Listing Owner 可進入提案頁面協商或回應（accept/reject）。

例外 / 替代流程:
- 若 Proposer 選取的某件物品已被其他提案鎖定，系統回傳錯誤並提示該物不可選；Proposer 可移除該項再送出。
- 若 Listing Owner 未在 TTL 內回覆，Proposal 自動標記為 EXPIRED 並釋放鎖定。

後置條件 / 產出:
- 成功建立 Proposal 並暫時鎖定 Proposer 的物品；Proposal id 與摘要存於系統。

關聯 UI / API:
- POST `/proposals`，GET `/proposals/{id}`，POST `/proposals/{id}/respond`

相關資料 / 實體:
- proposals(id, proposer_id, proposee_listing_id, proposer_listing_ids, status, expires_at)
- listings.locked_by_proposal_id

可度量的驗收標準（Gherkin）:
1. Given Proposer 與 Listing Owner 已登入，且所選物件皆可上鎖
   When Proposer 送出提案
   Then 系統建立 Proposal 並將所選物件 locked_by_proposal_id 設為 proposal id

2. Given Proposer 選的其中一項已被鎖定
   When Proposer 嘗試送出提案
   Then 系統拒絕該項並提示使用者移除或稍後再試

3. Given Proposal 已建立並設定 expiresAt
   When expiresAt 到期且 Owner 未回應
   Then Proposal.status = EXPIRED 並釋放所有鎖定的 listing

4. Given Proposal 已建立
   When Owner 接受（Accept）提案
   Then Proposal.status 變更為 ACCEPTED_BY_B 並通知 Proposer

備註:
- 建議在建立 proposal 的 transaction 中使用 optimistic lock 或 row-level lock，避免 race condition。
