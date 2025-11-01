# Entity 與 Service 業務邏輯檢查報告

**檢查日期**: 2025-11-01  
**檢查範圍**: Entity 層與 Service 層  
**對照標準**: Use Case 文件 (UC-04, UC-06, UC-08)

---

##  嚴重問題（Critical Issues）

### 1. **缺少 Listing 鎖定機制**  HIGH PRIORITY

**問題描述**:  
根據 UC-04，當 Proposal 建立時，必須鎖定相關的 Listing，避免同一張卡片被多個 Proposal 同時使用。

**現況**:
-  Listing entity 缺少 lockedByProposalId 欄位
-  ProposalService.createProposal() 沒有鎖定 Listing 的邏輯
-  沒有釋放鎖定的機制（當 Proposal 被拒絕/取消/過期時）

**UC-04 需求**:
`
2. 系統於 DB transaction 中檢查 Proposer 選取物是否為 ACTIVE 且未鎖定；
   若可用，建立 Proposal 並為該物件設定 locked_by_proposal_id。
`

**應該實作**:
`java
// Listing.java 需要新增
@Column(name = "locked_by_proposal_id")
private Long lockedByProposalId;

public void lockForProposal(Long proposalId) {
    if (this.lockedByProposalId != null) {
        throw new BusinessRuleViolationException("Listing already locked");
    }
    this.lockedByProposalId = proposalId;
}

public void unlock() {
    this.lockedByProposalId = null;
}

public boolean isLocked() {
    return this.lockedByProposalId != null;
}
`

**影響**:
-  可能發生 Race Condition：同一張卡片被多個 Proposal 選中
-  資料不一致：無法追蹤哪些 Listing 正在被提案中

---

### 2. **缺少 Proposal 到期處理機制**  HIGH PRIORITY

**問題描述**:  
UC-04 要求當 Proposal 過期時，自動釋放鎖定的 Listing。

**現況**:
-  Proposal.isExpired() 方法存在
-  Proposal.expiresAt 欄位存在
-  沒有定時任務（Scheduled Job）檢查並處理過期的 Proposal
-  ProposalRepository 缺少查詢方法

**UC-04 需求**:
`
若 Listing Owner 未在 TTL 內回覆，Proposal 自動標記為 EXPIRED 並釋放鎖定。
`

**應該實作**:
`java
// ProposalRepository.java - 新增查詢方法
List<Proposal> findByStatusAndExpiresAtBefore(
    ProposalStatus status, 
    LocalDateTime dateTime
);

// ProposalService.java - 新增定時任務
@Scheduled(fixedRate = 3600000) // 每小時執行
public void expireOverdueProposals() {
    List<Proposal> expiredProposals = proposalRepository
        .findByStatusAndExpiresAtBefore(
            Proposal.ProposalStatus.PENDING, 
            LocalDateTime.now()
        );
    
    for (Proposal proposal : expiredProposals) {
        proposal.cancel();  // 標記為過期
        
        // 釋放所有鎖定的 Listing
        for (ProposalItem item : proposal.getProposalItems()) {
            if (item.getSide() == ProposalItem.Side.PROPOSER) {
                item.getListing().unlock();
            }
        }
        proposalRepository.save(proposal);
    }
}
`

**影響**:
-  過期的 Proposal 永遠保持 PENDING 狀態
-  Listing 永久被鎖定，無法用於其他交易

---

### 3. **Swap 建立後缺少 Listing 狀態更新**  MEDIUM

**問題描述**:  
當 Proposal 被接受並建立 Swap 後，相關的 Listing 應該標記為 TRADED。

**現況**:
-  SwapService.createSwap() 只建立 Swap，沒有更新 Listing 狀態
-  ListingService 有 markAsTraded() 但 SwapService 沒呼叫

**應該實作**:
`java
// SwapService.createSwap()
public Swap createSwap(Long proposalId) {
    Proposal proposal = proposalService.getProposalById(proposalId);
    
    // 建立 Swap...
    Swap swap = swapRepository.save(/* ... */);
    
    //  關鍵修正：更新所有相關 Listing 為 TRADED
    for (ProposalItem item : proposal.getProposalItems()) {
        listingService.markAsTraded(item.getListing().getId());
    }
    
    return swap;
}
`

**影響**:
-  已交易的 Listing 仍顯示為 ACTIVE，可能被其他人選中
-  資料不一致

---

##  Phase 1 修正清單（立即執行）

###  修正項目

1. **Listing.java** - 新增鎖定欄位與方法
2. **ProposalService.java** - 實作鎖定邏輯
3. **ProposalRepository.java** - 新增查詢方法
4. **ProposalService.java** - 實作定時任務
5. **SwapService.java** - 新增 Listing 狀態更新
6. **ExchangeWebAppApplication.java** - 啟用排程

---

##  預期效果

修正後：
- **業務邏輯完整度**: 70  85
- **資料一致性**: 60  80
- **Use Case 符合度**: 65  80

---

**結論**: 立即執行 Phase 1 修正，確保核心業務邏輯正確性。
