package com.exchange.tests;

import com.exchange.platform.ExchangeWebAppApplication;
import com.exchange.platform.entity.*;
import com.exchange.platform.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ============================================================================
 * DeliverySystemTest - 送達確認（雙方簽收）模組測試
 * ============================================================================
 * 
 * <h2>測試範圍 (Scope)</h2>
 * 本測試類別專注於驗證「交換平台」的送達確認（Delivery Confirmation）功能，涵蓋：
 * - 單方確認（Single-Party Confirmation）
 * - 雙方確認完成交易（Both-Party Completion）
 * - 越權確認阻止（Unauthorized Access Prevention）
 * - 冪等性行為（Idempotent Behavior）
 * 
 * <h2>業務規則 (Business Rules)</h2>
 * <h3>1. 送達確認流程</h3>
 * <ul>
 *   <li><b>單方確認：</b> 當 User A 確認收貨時，設置 aConfirmedAt = 當前時間，Swap 狀態仍為 IN_PROGRESS</li>
 *   <li><b>雙方確認：</b> 當雙方都確認（aConfirmedAt != null && bConfirmedAt != null）時，Swap 狀態變更為 COMPLETED</li>
 *   <li><b>交易完成：</b> Swap 狀態=COMPLETED 時，設置 completedAt 時間，聊天室轉為唯讀，物品狀態更新為 SOLD/EXCHANGED</li>
 *   <li><b>冪等性：</b> 同一用戶重複確認不會出錯，第二次呼叫不會修改 confirmAt 時間，直接返回當前狀態</li>
 * </ul>
 * 
 * <h3>2. 權限控制</h3>
 * <ul>
 *   <li><b>參與者限制：</b> 只有 Swap 的 aUserId 或 bUserId 可以確認收貨</li>
 *   <li><b>非參與者：</b> 其他用戶嘗試確認  403 Forbidden</li>
 *   <li><b>未登入：</b> 未登入用戶嘗試確認  401 Unauthorized</li>
 * </ul>
 * 
 * <h3>3. 狀態管理</h3>
 * <ul>
 *   <li><b>IN_PROGRESS：</b> 初始狀態，配送進行中</li>
 *   <li><b>COMPLETED：</b> 雙方確認收貨後，交易完成</li>
 *   <li><b>aConfirmedAt/bConfirmedAt：</b> 記錄各自的確認時間（NULL=未確認，LocalDateTime=已確認）</li>
 *   <li><b>completedAt：</b> 交易完成時間，當雙方確認後自動設置</li>
 * </ul>
 * 
 * <h2>實作細節參考 (Implementation References)</h2>
 * <h3>Controller</h3>
 * <ul>
 *   <li><b>SwapController:</b> POST /api/swaps/{id}/confirm-received</li>
 *   <li><b>異常處理：</b> UnauthorizedException (401), ForbiddenException (403), NotFoundException (404)</li>
 * </ul>
 * 
 * <h3>Service</h3>
 * <ul>
 *   <li><b>SwapService.confirmReceived(id, session):</b>
 *     <ul>
 *       <li>驗證 session userId，若為空則拋出 UnauthorizedException</li>
 *       <li>根據 swapId 查詢 Swap，不存在則拋出 NotFoundException</li>
 *       <li>驗證 userId 是否為 aUserId 或 bUserId，否則拋出 ForbiddenException</li>
 *       <li>判斷是 User A 還是 User B：isA = swap.getAUserId().equals(userId)</li>
 *       <li>冪等性檢查：若 aConfirmedAt/bConfirmedAt 已存在，跳過設置（不重複更新時間）</li>
 *       <li>設置確認時間：isA ? setAConfirmedAt(now) : setBConfirmedAt(now)</li>
 *       <li>雙方確認檢查：若 aConfirmedAt != null && bConfirmedAt != null，則設置 status=COMPLETED, completedAt=now</li>
 *       <li>聊天室唯讀：當狀態變為 COMPLETED 時，呼叫 chatService.setReadOnly(swap.getId())</li>
 *       <li>物品狀態更新：呼叫 finalizeListingsForCompletedSwap(swap) 更新相關物品狀態</li>
 *       <li>保存並返回 SwapDTO</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>Entity</h3>
 * <ul>
 *   <li><b>Swap:</b> id, aUserId, bUserId, status (IN_PROGRESS/COMPLETED), aConfirmedAt, bConfirmedAt, completedAt</li>
 *   <li><b>Status Enum:</b> PENDING, IN_PROGRESS, COMPLETED, CANCELED</li>
 * </ul>
 * 
 * <h3>DTO</h3>
 * <ul>
 *   <li><b>SwapDTO:</b> 包含 aConfirmedAt, bConfirmedAt, status, completedAt 等欄位</li>
 * </ul>
 * 
 * <h2>測試策略 (Testing Strategy)</h2>
 * <h3>核心測試案例（Core Test Cases）</h3>
 * <ul>
 *   <li><b>TC-DL01:</b> User A 確認收貨，User B 未確認  aConfirmedAt 設置，狀態仍為 IN_PROGRESS</li>
 *   <li><b>TC-DL02:</b> User A 和 User B 都確認收貨  雙方 confirmAt 設置，狀態變為 COMPLETED，completedAt 設置</li>
 *   <li><b>TC-DL03:</b> 非參與者（User C）嘗試確認  403 Forbidden，無記錄變更</li>
 * </ul>
 * 
 * <h3>輔助測試（Auxiliary Tests）</h3>
 * <ul>
 *   <li><b>Auxiliary 1:</b> 未登入用戶嘗試確認  401 Unauthorized</li>
 *   <li><b>Auxiliary 2:</b> 冪等性測試 - 同一用戶重複確認  第二次呼叫成功，confirmAt 時間不變</li>
 *   <li><b>Auxiliary 3:</b> User B 先確認，User A 後確認  順序不影響結果，雙方確認後完成交易</li>
 * </ul>
 * 
 * <h2>測試資料準備 (Test Data Setup)</h2>
 * <ul>
 *   <li><b>userA:</b> 交換發起人（listing owner），userId 用於 swap.aUserId</li>
 *   <li><b>userB:</b> 交換接受者（proposer），userId 用於 swap.bUserId</li>
 *   <li><b>userC:</b> 非參與者，用於越權測試</li>
 *   <li><b>listing:</b> userA 的物品清單</li>
 *   <li><b>proposal:</b> userB 提出的交換提議（狀態=ACCEPTED）</li>
 *   <li><b>swap:</b> 交換記錄（狀態=IN_PROGRESS，aConfirmedAt=null, bConfirmedAt=null）</li>
 *   <li><b>sessionA, sessionB, sessionC:</b> 模擬三個用戶的 HTTP session</li>
 * </ul>
 * 
 * <h2>注意事項 (Notes)</h2>
 * <ul>
 *   <li><b>UTF-8 編碼：</b> 檔案必須使用 UTF-8 without BOM 以避免編譯錯誤</li>
 *   <li><b>不刪除用戶：</b> setUp() 不應刪除所有用戶，因為其他表格有 FK 約束</li>
 *   <li><b>時間敏感：</b> confirmAt 時間欄位在冪等性測試中需要精確比對，確保第二次呼叫不會修改時間</li>
 *   <li><b>聊天室唯讀：</b> 交易完成後聊天室應設為唯讀，但此行為由 ChatService 處理，本測試僅驗證 Swap 狀態</li>
 *   <li><b>物品狀態：</b> 交易完成後相關物品狀態應更新，但此行為由 finalizeListingsForCompletedSwap 處理</li>
 * </ul>
 * 
 * @author Exchange Platform Test Team
 * @version 1.0
 * @since 2025-01-15
 */
@SpringBootTest(classes = ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@Transactional
public class DeliverySystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private SwapRepository swapRepository;

    private User userA;
    private User userB;
    private User userC;
    private Listing listing;
    private Proposal proposal;
    private Swap swap;
    private MockHttpSession sessionA;
    private MockHttpSession sessionB;
    private MockHttpSession sessionC;

    @BeforeEach
    void setUp() {
        // Create User A (listing owner, swap participant)
        userA = new User();
        userA.setEmail("userA@test.com");
        userA.setPasswordHash("hash");
        userA.setDisplayName("User A");
        userA.setCreatedAt(LocalDateTime.now());
        userA.setUpdatedAt(LocalDateTime.now());
        userA = userRepository.save(userA);

        // Create User B (proposer, swap participant)
        userB = new User();
        userB.setEmail("userB@test.com");
        userB.setPasswordHash("hash");
        userB.setDisplayName("User B");
        userB.setCreatedAt(LocalDateTime.now());
        userB.setUpdatedAt(LocalDateTime.now());
        userB = userRepository.save(userB);

        // Create User C (non-participant, for unauthorized tests)
        userC = new User();
        userC.setEmail("userC@test.com");
        userC.setPasswordHash("hash");
        userC.setDisplayName("User C");
        userC.setCreatedAt(LocalDateTime.now());
        userC.setUpdatedAt(LocalDateTime.now());
        userC = userRepository.save(userC);

        // Create Listing for User A
        listing = new Listing();
        listing.setUserId(userA.getId());
        listing.setCardName("Test Card A");
        listing.setArtistName("Artist A");
        listing.setCardSource(Listing.CardSource.ALBUM);
        listing.setConditionRating(8);
        listing.setHasProtection(true);
        listing.setImagePaths("[\"test.jpg\"]");
        listing.setDescription("Test description");
        listing.setStatus(Listing.Status.AVAILABLE);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);

        // Create Proposal from User B
        proposal = new Proposal();
        proposal.setListingId(listing.getId());
        proposal.setReceiverId(userA.getId()); // Listing owner is the receiver
        proposal.setProposerId(userB.getId());
        proposal.setMessage("I want to exchange");
        proposal.setStatus(Proposal.Status.ACCEPTED);
        proposal.setCreatedAt(LocalDateTime.now());
        proposal.setUpdatedAt(LocalDateTime.now());
        proposal = proposalRepository.save(proposal);

        // Create Swap in IN_PROGRESS status
        swap = new Swap();
        swap.setListingId(listing.getId());
        swap.setProposalId(proposal.getId());
        swap.setAUserId(userA.getId());
        swap.setBUserId(userB.getId());
        swap.setStatus(Swap.Status.IN_PROGRESS);
        swap.setCreatedAt(LocalDateTime.now());
        swap.setUpdatedAt(LocalDateTime.now());
        swap.setAConfirmedAt(null);
        swap.setBConfirmedAt(null);
        swap.setCompletedAt(null);
        swap = swapRepository.save(swap);

        // Create mock sessions
        sessionA = new MockHttpSession();
        sessionA.setAttribute("userId", userA.getId());

        sessionB = new MockHttpSession();
        sessionB.setAttribute("userId", userB.getId());

        sessionC = new MockHttpSession();
        sessionC.setAttribute("userId", userC.getId());
    }

    @Test
    @DisplayName("TC-DL01: 單方先確認 - User A 確認收貨，User B 未確認")
    void testSinglePartyConfirmation() throws Exception {
        // Given: Swap is IN_PROGRESS, no confirmations yet
        System.out.println("\n===== [TC-DL01] 測試開始：User A 單方確認收貨 =====");
        System.out.println("[Given] Swap ID: " + swap.getId() + ", Status: " + swap.getStatus());
        System.out.println("[Given] aConfirmedAt: " + swap.getAConfirmedAt() + ", bConfirmedAt: " + swap.getBConfirmedAt());

        // When: User A confirms received
        System.out.println("[When] User A (ID=" + userA.getId() + ") 呼叫 POST /api/swaps/" + swap.getId() + "/confirm-received");
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.aConfirmedAt").exists())
                .andExpect(jsonPath("$.bConfirmedAt").doesNotExist());

        // Then: Verify database state
        Swap updated = swapRepository.findById(swap.getId()).orElseThrow();
        System.out.println("[Then] Swap 狀態更新成功：");
        System.out.println("  - Status: " + updated.getStatus() + " (預期：IN_PROGRESS)");
        System.out.println("  - aConfirmedAt: " + updated.getAConfirmedAt() + " (預期：非 null)");
        System.out.println("  - bConfirmedAt: " + updated.getBConfirmedAt() + " (預期：null)");
        System.out.println("  - completedAt: " + updated.getCompletedAt() + " (預期：null)");

        assertThat(updated.getStatus()).isEqualTo(Swap.Status.IN_PROGRESS);
        assertThat(updated.getAConfirmedAt()).isNotNull();
        assertThat(updated.getBConfirmedAt()).isNull();
        assertThat(updated.getCompletedAt()).isNull();

        System.out.println(" TC-DL01: 單方確認成功，status=IN_PROGRESS，awaiting-counterparty");
    }

    @Test
    @DisplayName("TC-DL02: 雙方完成 - User A 和 User B 都確認收貨，交易完成")
    void testBothPartiesConfirmation() throws Exception {
        // Given: User A already confirmed
        System.out.println("\n===== [TC-DL02] 測試開始：雙方確認收貨，交易完成 =====");
        System.out.println("[Given] User A 先確認收貨...");
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionA))
                .andExpect(status().isOk());

        Swap afterA = swapRepository.findById(swap.getId()).orElseThrow();
        System.out.println("[Given] User A 確認後狀態：");
        System.out.println("  - aConfirmedAt: " + afterA.getAConfirmedAt());
        System.out.println("  - bConfirmedAt: " + afterA.getBConfirmedAt());
        System.out.println("  - status: " + afterA.getStatus());

        // When: User B confirms received
        System.out.println("[When] User B (ID=" + userB.getId() + ") 確認收貨...");
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.aConfirmedAt").exists())
                .andExpect(jsonPath("$.bConfirmedAt").exists())
                .andExpect(jsonPath("$.completedAt").exists());

        // Then: Verify both confirmed and swap completed
        Swap completed = swapRepository.findById(swap.getId()).orElseThrow();
        System.out.println("[Then] 雙方確認完成，交易狀態變更：");
        System.out.println("  - Status: " + completed.getStatus() + " (預期：COMPLETED)");
        System.out.println("  - aConfirmedAt: " + completed.getAConfirmedAt() + " (預期：非 null)");
        System.out.println("  - bConfirmedAt: " + completed.getBConfirmedAt() + " (預期：非 null)");
        System.out.println("  - completedAt: " + completed.getCompletedAt() + " (預期：非 null)");

        assertThat(completed.getStatus()).isEqualTo(Swap.Status.COMPLETED);
        assertThat(completed.getAConfirmedAt()).isNotNull();
        assertThat(completed.getBConfirmedAt()).isNotNull();
        assertThat(completed.getCompletedAt()).isNotNull();

        System.out.println(" TC-DL02: 雙方確認成功，status=COMPLETED，交易完成，後續評價可進行");
    }

    @Test
    @DisplayName("TC-DL03: 越權確認 - 非交易雙方嘗試確認，應返回 403 Forbidden")
    void testUnauthorizedConfirmation() throws Exception {
        // Given: User C is not a participant
        System.out.println("\n===== [TC-DL03] 測試開始：非參與者越權確認 =====");
        System.out.println("[Given] Swap 參與者：User A (ID=" + userA.getId() + "), User B (ID=" + userB.getId() + ")");
        System.out.println("[Given] 嘗試確認者：User C (ID=" + userC.getId() + ") [非參與者]");

        // When: User C tries to confirm
        System.out.println("[When] User C 嘗試呼叫 POST /api/swaps/" + swap.getId() + "/confirm-received");
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionC))
                .andExpect(status().isForbidden());

        // Then: Verify no changes in database
        Swap unchanged = swapRepository.findById(swap.getId()).orElseThrow();
        System.out.println("[Then] 確認被拒絕，資料庫狀態未變更：");
        System.out.println("  - aConfirmedAt: " + unchanged.getAConfirmedAt() + " (預期：null)");
        System.out.println("  - bConfirmedAt: " + unchanged.getBConfirmedAt() + " (預期：null)");
        System.out.println("  - status: " + unchanged.getStatus() + " (預期：IN_PROGRESS)");

        assertThat(unchanged.getAConfirmedAt()).isNull();
        assertThat(unchanged.getBConfirmedAt()).isNull();
        assertThat(unchanged.getStatus()).isEqualTo(Swap.Status.IN_PROGRESS);

        System.out.println(" TC-DL03: 越權確認被正確拒絕，回傳 403 Forbidden，審計記錄應包含此次嘗試");
    }

    @Test
    @DisplayName("輔助測試 1: 未登入用戶嘗試確認，應返回 401 Unauthorized")
    void testUnauthenticatedConfirmation() throws Exception {
        // Given: No session
        System.out.println("\n===== [輔助測試 1] 測試開始：未登入用戶嘗試確認 =====");
        System.out.println("[Given] 使用空白 session（未登入狀態）");

        // When: Unauthenticated user tries to confirm
        System.out.println("[When] 呼叫 POST /api/swaps/" + swap.getId() + "/confirm-received (無 session)");
        MockHttpSession emptySession = new MockHttpSession();
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(emptySession))
                .andExpect(status().isUnauthorized());

        // Then: Verify no changes
        Swap unchanged = swapRepository.findById(swap.getId()).orElseThrow();
        System.out.println("[Then] 確認被拒絕，資料庫狀態未變更：");
        System.out.println("  - aConfirmedAt: " + unchanged.getAConfirmedAt() + " (預期：null)");
        System.out.println("  - bConfirmedAt: " + unchanged.getBConfirmedAt() + " (預期：null)");

        assertThat(unchanged.getAConfirmedAt()).isNull();
        assertThat(unchanged.getBConfirmedAt()).isNull();

        System.out.println(" 輔助測試 1: 未登入用戶確認被正確拒絕，回傳 401 Unauthorized");
    }

    @Test
    @DisplayName("輔助測試 2: 冪等性測試 - 同一用戶重複確認，不應修改 confirmAt 時間")
    void testIdempotentConfirmation() throws Exception {
        // Given: User A confirms first time
        System.out.println("\n===== [輔助測試 2] 測試開始：冪等性測試 =====");
        System.out.println("[Given] User A 第一次確認收貨...");
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionA))
                .andExpect(status().isOk());

        Swap afterFirst = swapRepository.findById(swap.getId()).orElseThrow();
        LocalDateTime firstConfirmTime = afterFirst.getAConfirmedAt();
        System.out.println("[Given] 第一次確認時間：" + firstConfirmTime);

        // When: User A confirms second time
        System.out.println("[When] User A 第二次確認收貨（重複呼叫）...");
        Thread.sleep(100); // Ensure time difference if timestamp were to change
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionA))
                .andExpect(status().isOk());

        // Then: Verify confirmAt time unchanged
        Swap afterSecond = swapRepository.findById(swap.getId()).orElseThrow();
        LocalDateTime secondConfirmTime = afterSecond.getAConfirmedAt();
        System.out.println("[Then] 第二次確認時間：" + secondConfirmTime);
        System.out.println("[Then] 時間比對：" + firstConfirmTime + " == " + secondConfirmTime);

        assertThat(secondConfirmTime).isEqualTo(firstConfirmTime);

        System.out.println(" 輔助測試 2: 冪等性測試通過，重複確認不修改時間");
    }

    @Test
    @DisplayName("輔助測試 3: User B 先確認，User A 後確認，順序不影響結果")
    void testReversedConfirmationOrder() throws Exception {
        // Given: User B confirms first
        System.out.println("\n===== [輔助測試 3] 測試開始：反向確認順序 =====");
        System.out.println("[Given] User B 先確認收貨...");
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        Swap afterB = swapRepository.findById(swap.getId()).orElseThrow();
        System.out.println("[Given] User B 確認後狀態：");
        System.out.println("  - bConfirmedAt: " + afterB.getBConfirmedAt());
        System.out.println("  - status: " + afterB.getStatus());

        // When: User A confirms second
        System.out.println("[When] User A 後確認收貨...");
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/confirm-received")
                        .session(sessionA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Then: Verify swap completed regardless of order
        Swap completed = swapRepository.findById(swap.getId()).orElseThrow();
        System.out.println("[Then] 雙方確認完成（反向順序）：");
        System.out.println("  - Status: " + completed.getStatus() + " (預期：COMPLETED)");
        System.out.println("  - aConfirmedAt: " + completed.getAConfirmedAt());
        System.out.println("  - bConfirmedAt: " + completed.getBConfirmedAt());

        assertThat(completed.getStatus()).isEqualTo(Swap.Status.COMPLETED);
        assertThat(completed.getAConfirmedAt()).isNotNull();
        assertThat(completed.getBConfirmedAt()).isNotNull();
        assertThat(completed.getCompletedAt()).isNotNull();

        System.out.println(" 輔助測試 3: 反向確認順序測試通過，順序不影響交易完成結果");
    }
}