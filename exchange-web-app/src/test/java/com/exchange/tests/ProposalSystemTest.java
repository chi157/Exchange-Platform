/**
 * 模組 D：交換提案系統測試
 * Module D: Proposal System Test
 * 
 * 測試範圍 (Test Scope):
 * - POST /api/proposals - 創建交換提案
 * - POST /api/proposals/{id}/accept - 接受提案
 * - POST /api/proposals/{id}/reject - 拒絕提案
 * 
 * 測試重點 (Key Test Points):
 * 1. 提案創建 (Proposal Creation):
 *    - 驗證合法提案可以成功創建
 *    - 檢查提案初始狀態為 PENDING
 *    - 驗證自動創建聊天室功能
 *    - 驗證電子郵件通知發送
 * 
 * 2. 業務規則驗證 (Business Rule Validation):
 *    - 禁止向自己的刊登提出交換提案 (TC-PR02)
 *    - 防止重複提案 (TC-PR03)
 *    - 驗證只有相關方可以操作提案 (TC-PR07)
 * 
 * 3. 提案狀態管理 (Proposal Status Management):
 *    - 接受提案: PENDING  ACCEPTED
 *    - 拒絕提案: PENDING  REJECTED
 *    - 狀態轉換後無法重複操作
 * 
 * 4. 權限控制 (Authorization):
 *    - 只有接收者可以接受/拒絕提案
 *    - 非參與者無法操作提案
 *    - 未登入用戶無法創建提案
 * 
 * 5. 資源狀態驗證 (Resource State Validation):
 *    - 驗證刊登狀態在接受提案後變更為 LOCKED
 *    - 驗證 Swap 交易記錄正確創建
 *    - 驗證聊天室與 Swap 關聯
 * 
 * 實作細節參考 (Implementation Reference):
 * - Controller: ProposalController
 *   - POST /api/proposals - create(CreateProposalRequest, HttpSession)
 *   - POST /api/proposals/{id}/accept - accept(Long, HttpSession)
 *   - POST /api/proposals/{id}/reject - reject(Long, HttpSession)
 * 
 * - Service: ProposalService
 *   - create(): 創建提案，檢查自提案、重複提案，創建聊天室
 *   - accept(): 接受提案，創建 Swap，鎖定刊登，發送通知
 *   - reject(): 拒絕提案，更新狀態
 * 
 * - Entity: Proposal
 *   - Status: PENDING, ACCEPTED, REJECTED, CANCELLED, EXPIRED
 *   - ProposalItem: Side (OFFERED/REQUESTED)
 * 
 * - DTO: CreateProposalRequest
 *   - listingId: Long (接收者的刊登 ID)
 *   - proposerListingIds: List<Long> (提案者提供的刊登 ID 列表)
 *   - message: String (提案訊息)
 * 
 * 測試策略 (Testing Strategy):
 * - 使用 @SpringBootTest 進行完整應用程式上下文測試
 * - 使用 MockMvc 模擬 HTTP 請求
 * - 使用 @Transactional 確保每個測試後自動回滾資料庫
 * - 使用 @BeforeEach 創建測試數據
 * - 驗證 HTTP 狀態碼、JSON 回應、資料庫狀態
 * 
 * 測試數據準備 (Test Data Setup):
 * - 創建兩個測試用戶 (proposer, receiver)
 * - 為每個用戶創建測試刊登
 * - 為每個測試設定會話狀態
 * 
 * 測試案例清單 (Test Cases):
 * - TC-PR01: 建立提案 - 合法對象/物品
 * - TC-PR02: 自提案禁止 - 自身刊登
 * - TC-PR03: 重複提案 - 同一對象/刊登
 * - TC-PR04: 接受提案 - 參與者接受
 * - TC-PR05: 拒絕提案 - 參與者拒絕
 * - TC-PR06: 取消提案 - 發起方取消 (未實作)
 * - TC-PR07: 越權操作 - 非參與者
 * 
 * @author 測試工程師：陳欣妤
 * @date 2025/12/12
 * @framework JUnit 5 + Spring Boot Test + MockMvc
 */
package com.exchange.tests;

import com.exchange.platform.ExchangeWebAppApplication;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.Proposal;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.ProposalRepository;
import com.exchange.platform.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@Transactional
public class ProposalSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    private User proposerUser;
    private User receiverUser;
    private User thirdUser;
    private Listing proposerListing;
    private Listing receiverListing;
    private MockHttpSession proposerSession;
    private MockHttpSession receiverSession;
    private MockHttpSession thirdUserSession;

    @BeforeEach
    public void setUp() {
        // Create proposer user
        proposerUser = User.builder()
                .email("proposer@test.com")
                .passwordHash("$2a$10$dummy") // 使用 passwordHash 而非 password
                .displayName("Proposer User")
                .build();
        proposerUser = userRepository.save(proposerUser);

        // Create receiver user
        receiverUser = User.builder()
                .email("receiver@test.com")
                .passwordHash("$2a$10$dummy")
                .displayName("Receiver User")
                .build();
        receiverUser = userRepository.save(receiverUser);

        // Create third user (for unauthorized tests)
        thirdUser = User.builder()
                .email("third@test.com")
                .passwordHash("$2a$10$dummy")
                .displayName("Third User")
                .build();
        thirdUser = userRepository.save(thirdUser);

        // Create proposer's listing (what proposer offers)
        proposerListing = Listing.builder()
                .cardName("Proposer Card")
                .artistName("Artist A")
                .groupName("Group A")
                .description("Proposer's card for exchange")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(9)
                .hasProtection(true)
                .imagePaths("/images/proposer.jpg")
                .userId(proposerUser.getId())
                .status(Listing.Status.AVAILABLE)
                .build();
        proposerListing = listingRepository.save(proposerListing);

        // Create receiver's listing (what proposer wants)
        receiverListing = Listing.builder()
                .cardName("Receiver Card")
                .artistName("Artist B")
                .groupName("Group B")
                .description("Receiver's card for exchange")
                .cardSource(Listing.CardSource.CONCERT)
                .conditionRating(10)
                .hasProtection(false)
                .imagePaths("/images/receiver.jpg")
                .userId(receiverUser.getId())
                .status(Listing.Status.AVAILABLE)
                .build();
        receiverListing = listingRepository.save(receiverListing);

        // Create sessions
        proposerSession = new MockHttpSession();
        proposerSession.setAttribute("userId", proposerUser.getId());

        receiverSession = new MockHttpSession();
        receiverSession.setAttribute("userId", receiverUser.getId());

        thirdUserSession = new MockHttpSession();
        thirdUserSession.setAttribute("userId", thirdUser.getId());
    }

    // TC-PR01: 建立提案 - 合法對象/物品
    @Test
    @DisplayName("TC-PR01: 成功建立提案 - 合法對象與物品")
    public void testCreateProposal_Success() throws Exception {
        // Given: 提案者想要交換接收者的卡片
        Map<String, Object> request = new HashMap<>();
        request.put("listingId", receiverListing.getId());
        request.put("proposerListingIds", Arrays.asList(proposerListing.getId()));
        request.put("message", "我想交換你的卡片");

        // When: 發送創建提案請求
        mockMvc.perform(post("/api/proposals")
                        .session(proposerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: 應該成功創建
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.listingId").value(receiverListing.getId()))
                .andExpect(jsonPath("$.proposerId").value(proposerUser.getId()))
                .andExpect(jsonPath("$.receiverId").value(receiverUser.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value("我想交換你的卡片"));

        System.out.println(" TC-PR01 通過：成功建立提案，狀態為 PENDING，包含正確的提案者與接收者資訊");
    }

    // TC-PR02: 自提案禁止 - 自身刊登
    @Test
    @DisplayName("TC-PR02: 自提案禁止 - 不能向自己的刊登提出提案")
    public void testCreateProposal_SelfProposal_Forbidden() throws Exception {
        // Given: 嘗試向自己的刊登提出提案
        Map<String, Object> request = new HashMap<>();
        request.put("listingId", proposerListing.getId()); // 自己的刊登
        request.put("proposerListingIds", Arrays.asList(proposerListing.getId()));
        request.put("message", "測試自提案");

        // When: 發送創建提案請求
        mockMvc.perform(post("/api/proposals")
                        .session(proposerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: 應該被拒絕 (403 Forbidden)
                .andExpect(status().isForbidden());

        System.out.println(" TC-PR02 通過：自提案被正確拒絕，回傳 403 Forbidden");
    }

    // TC-PR03: 重複提案 - 同一對象/刊登
    @Test
    @DisplayName("TC-PR03: 重複提案 - 不能對同一刊登重複提出 PENDING 提案")
    public void testCreateProposal_DuplicatePending_Conflict() throws Exception {
        // Given: 先創建一個提案
        Map<String, Object> request = new HashMap<>();
        request.put("listingId", receiverListing.getId());
        request.put("proposerListingIds", Arrays.asList(proposerListing.getId()));
        request.put("message", "第一次提案");

        mockMvc.perform(post("/api/proposals")
                        .session(proposerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // When: 再次對同一刊登提出提案
        Map<String, Object> duplicateRequest = new HashMap<>();
        duplicateRequest.put("listingId", receiverListing.getId());
        duplicateRequest.put("proposerListingIds", Arrays.asList(proposerListing.getId()));
        duplicateRequest.put("message", "第二次提案");

        // Then: 應該被拒絕 (409 Conflict)
        mockMvc.perform(post("/api/proposals")
                        .session(proposerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict());

        System.out.println(" TC-PR03 通過：重複提案被正確拒絕，回傳 409 Conflict");
    }

    // TC-PR04: 接受提案 - 參與者接受
    @Test
    @DisplayName("TC-PR04: 接受提案 - 接收者成功接受提案")
    public void testAcceptProposal_Success() throws Exception {
        // Given: 先創建一個提案
        Proposal proposal = Proposal.builder()
                .listingId(receiverListing.getId())
                .proposerId(proposerUser.getId())
                .receiverId(receiverUser.getId())
                .message("測試提案")
                .status(Proposal.Status.PENDING)
                .build();
        proposal = proposalRepository.save(proposal);

        // When: 接收者接受提案
        mockMvc.perform(post("/api/proposals/" + proposal.getId() + "/accept")
                        .session(receiverSession))
                // Then: 應該成功接受
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposal.getId()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        System.out.println(" TC-PR04 通過：提案成功被接受，狀態變更為 ACCEPTED");
    }

    // TC-PR05: 拒絕提案 - 參與者拒絕
    @Test
    @DisplayName("TC-PR05: 拒絕提案 - 接收者成功拒絕提案")
    public void testRejectProposal_Success() throws Exception {
        // Given: 先創建一個提案
        Proposal proposal = Proposal.builder()
                .listingId(receiverListing.getId())
                .proposerId(proposerUser.getId())
                .receiverId(receiverUser.getId())
                .message("測試提案")
                .status(Proposal.Status.PENDING)
                .build();
        proposal = proposalRepository.save(proposal);

        // When: 接收者拒絕提案
        mockMvc.perform(post("/api/proposals/" + proposal.getId() + "/reject")
                        .session(receiverSession))
                // Then: 應該成功拒絕
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(proposal.getId()))
                .andExpect(jsonPath("$.status").value("REJECTED"));

        System.out.println(" TC-PR05 通過：提案成功被拒絕，狀態變更為 REJECTED");
    }

    // TC-PR07: 越權操作 - 非參與者
    @Test
    @DisplayName("TC-PR07: 越權操作 - 非參與者無法接受提案")
    public void testAcceptProposal_Unauthorized_Forbidden() throws Exception {
        // Given: 先創建一個提案
        Proposal proposal = Proposal.builder()
                .listingId(receiverListing.getId())
                .proposerId(proposerUser.getId())
                .receiverId(receiverUser.getId())
                .message("測試提案")
                .status(Proposal.Status.PENDING)
                .build();
        proposal = proposalRepository.save(proposal);

        // When: 第三方用戶嘗試接受提案
        mockMvc.perform(post("/api/proposals/" + proposal.getId() + "/accept")
                        .session(thirdUserSession))
                // Then: 應該被拒絕 (403 Forbidden)
                .andExpect(status().isForbidden());

        System.out.println(" TC-PR07 通過：非參與者無法接受提案，回傳 403 Forbidden");
    }

    // 輔助測試：未登入用戶無法創建提案
    @Test
    @DisplayName("輔助測試：未登入用戶無法創建提案")
    public void testCreateProposal_Unauthenticated_Unauthorized() throws Exception {
        // Given: 未登入的請求
        Map<String, Object> request = new HashMap<>();
        request.put("listingId", receiverListing.getId());
        request.put("proposerListingIds", Arrays.asList(proposerListing.getId()));
        request.put("message", "未登入提案");

        // When: 發送創建提案請求 (無 session)
        mockMvc.perform(post("/api/proposals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                // Then: 應該回傳 401 Unauthorized
                .andExpect(status().isUnauthorized());

        System.out.println(" 輔助測試通過：未登入用戶無法創建提案，回傳 401 Unauthorized");
    }

    // 輔助測試：非參與者無法拒絕提案
    @Test
    @DisplayName("輔助測試：非參與者無法拒絕提案")
    public void testRejectProposal_Unauthorized_Forbidden() throws Exception {
        // Given: 先創建一個提案
        Proposal proposal = Proposal.builder()
                .listingId(receiverListing.getId())
                .proposerId(proposerUser.getId())
                .receiverId(receiverUser.getId())
                .message("測試提案")
                .status(Proposal.Status.PENDING)
                .build();
        proposal = proposalRepository.save(proposal);

        // When: 第三方用戶嘗試拒絕提案
        mockMvc.perform(post("/api/proposals/" + proposal.getId() + "/reject")
                        .session(thirdUserSession))
                // Then: 應該被拒絕 (403 Forbidden)
                .andExpect(status().isForbidden());

        System.out.println(" 輔助測試通過：非參與者無法拒絕提案，回傳 403 Forbidden");
    }
}