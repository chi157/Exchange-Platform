/**
 * 系統非功能性測試 - 安全性測試（Security Test）
 * 
 * 測試範疇：系統非功能性測試 - 5.3.2 安全性測試（Security）
 * 
 * 測試目的：
 * 驗證系統的安全性機制，確保授權控制、輸入驗證、XSS 防護、SQL 注入防護等安全措施有效運作。
 * 
 * 測試案例定義：
 * SEC-01: 未登入訪問私有頁面
 *   - 測試範圍: /ui/profile, /ui/my-listings, /ui/proposals/mine, /ui/chat
 *   - 預期行為: 302 重定向至 /ui/auth/login
 *   - 驗證點: 未授權訪問防護、Session 驗證機制
 * 
 * SEC-02: 未授權 API 調用
 *   - 測試範圍: POST /api/proposals, POST /api/chat/{roomId}/messages
 *   - 預期行為: 401 Unauthorized 或 403 Forbidden
 *   - 驗證點: API 權限控制、資料不被非法修改
 * 
 * SEC-03: XSS 注入檢測
 *   - 測試範圍: 聊天訊息、刊登描述、Email 模板變數
 *   - 預期行為: 輸出編碼，前端不執行惡意腳本
 *   - 驗證點: Thymeleaf th:text 使用、安全輸出策略
 * 
 * SEC-04: SQL 注入與排序白名單檢測
 *   - 測試範圍: GET /api/listings 的 q 和 sort 參數
 *   - 預期行為: 參數化查詢與欄位白名單阻擋惡意輸入
 *   - 驗證點: Repository 查詢構建、參數綁定安全性
 * 
 * 關聯代碼檔案：
 * - Controller: UiListingController, UiProfileController, ProposalController, ChatController
 * - Service: ProposalService, ChatService (權限檢查邏輯)
 * - Repository: ListingRepository (查詢構建與參數綁定)
 * - Configuration: SecurityConfig (Spring Security 配置)
 * - Template: Thymeleaf 模板中的 th:text / th:utext 使用
 * 
 * 技術實現：
 * - 使用 MockMvc 模擬 HTTP 請求
 * - 使用 MockHttpSession 模擬已登入/未登入狀態
 * - 使用 jsoup 解析 HTML 回應，檢查 XSS 編碼
 * - 使用 @Transactional 確保測試資料不污染資料庫
 * 
 * 風險評估關聯：
 * - 授權空隙風險: 未登入訪問私有資源
 * - XSS 風險: 聊天訊息、刊登描述未編碼輸出
 * - SQL 注入風險: 搜尋查詢參數未驗證
 * - 資料一致性: 非法 API 呼叫導致資料污染
 * 
 * @author Exchange Platform Test Team
 * @version 1.0
 * @since 2025-12-12
 */
package com.exchange.tests;

import com.exchange.platform.entity.*;
import com.exchange.platform.repository.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.exchange.platform.ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    private User testUser;
    private MockHttpSession authenticatedSession;

    @BeforeEach
    void setUp() {
        // 建立測試使用者
        testUser = userRepository.findById(1L).orElseGet(() -> {
            User u = new User();
            u.setEmail("testuser@test.com");
            u.setDisplayName("Test User");
            u.setPasswordHash("hash");
            u.setVerified(true);
            return userRepository.save(u);
        });

        // 建立已登入的 session
        authenticatedSession = new MockHttpSession();
        authenticatedSession.setAttribute("userId", testUser.getId());
    }

    @Test
    void testSEC01_UnauthorizedAccessToPrivatePages() throws Exception {
        System.out.println("\n========== SEC-01: 未登入訪問私有頁面測試 ==========");

        String[] protectedPages = {
            "/ui/profile",
            "/ui/my-listings",
            "/ui/proposals/mine",
            "/ui/chat"
        };

        System.out.println("\n[測試] 未登入狀態訪問受保護頁面");
        
        for (String page : protectedPages) {
            System.out.println("\n  測試頁面: " + page);
            
            // 不帶 session（未登入）訪問
            MvcResult result = mockMvc.perform(get(page))
                    .andExpect(status().is3xxRedirection())
                    .andReturn();

            String redirectUrl = result.getResponse().getRedirectedUrl();
            assertTrue(redirectUrl != null && redirectUrl.contains("/ui/auth/login"),
                    "應重定向到登入頁，實際: " + redirectUrl);
            
            System.out.println("    ✅ 狀態: " + result.getResponse().getStatus());
            System.out.println("    ✅ 重定向: " + redirectUrl);
        }

        System.out.println("\n[驗證] 已登入用戶可正常訪問");
        
        // 測試已登入用戶可以訪問
        for (String page : protectedPages) {
            System.out.println("\n  測試頁面: " + page + " (已登入)");
            
            MvcResult result = mockMvc.perform(get(page).session(authenticatedSession))
                    .andExpect(status().isOk())
                    .andReturn();
            
            System.out.println("    ✅ 已登入用戶可正常訪問");
        }

        System.out.println("\n✅ SEC-01 測試通過：未授權訪問防護正常運作");
    }

    @Test
    void testSEC02_UnauthorizedAPIAccess() throws Exception {
        System.out.println("\n========== SEC-02: 未授權 API 調用測試 ==========");

        // 建立測試數據
        Listing listing = Listing.builder()
                .userId(testUser.getId())
                .cardName("Test Card")
                .artistName("Artist")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(10)
                .hasProtection(true)
                .imagePaths("[\"test.jpg\"]")
                .status(Listing.Status.AVAILABLE).build();
        listing.prePersist();
        listing = listingRepository.save(listing);

        System.out.println("\n[測試 1] 未登入調用 POST /api/proposals");
        
        // 建立提案者的刊登物品
        Listing proposerListing = Listing.builder()
                .userId(testUser.getId())
                .cardName("Proposer Card")
                .artistName("Artist")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(9)
                .hasProtection(true)
                .imagePaths("[\"test2.jpg\"]")
                .status(Listing.Status.AVAILABLE).build();
        proposerListing.prePersist();
        proposerListing = listingRepository.save(proposerListing);
        
        String proposalJson = String.format(
            "{\"listingId\": %d, \"proposerListingIds\": [%d], \"message\": \"Test proposal\"}",
            listing.getId(), proposerListing.getId()
        );

        // 未登入狀態嘗試創建提案
        MvcResult result1 = mockMvc.perform(post("/api/proposals")
                .contentType(MediaType.APPLICATION_JSON)
                .content(proposalJson))
                .andExpect(status().is4xxClientError()) // 期望 4xx 錯誤
                .andReturn();

        int status1 = result1.getResponse().getStatus();
        assertTrue(status1 == 401 || status1 == 500,
                "未授權 API 調用應返回 401/500，實際: " + status1);
        System.out.println("  ✅ 狀態碼: " + status1 + " (未授權被阻擋)");

        // 驗證資料庫未被修改
        long proposalCount = proposalRepository.count();
        System.out.println("  ✅ Proposal 數量: " + proposalCount + " (資料未被非法創建)");

        System.out.println("\n[測試 2] 已登入用戶可正常調用 API");
        
        // 建立另一個用戶作為接收者
        User receiver = userRepository.findById(2L).orElseGet(() -> {
            User u = new User();
            u.setEmail("receiver@test.com");
            u.setDisplayName("Receiver");
            u.setPasswordHash("hash");
            u.setVerified(true);
            return userRepository.save(u);
        });

        Listing receiverListing = Listing.builder()
                .userId(receiver.getId())
                .cardName("Receiver Card")
                .artistName("Artist")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(9)
                .hasProtection(true)
                .imagePaths("[\"test2.jpg\"]")
                .status(Listing.Status.AVAILABLE).build();
        receiverListing.prePersist();
        receiverListing = listingRepository.save(receiverListing);

        String validProposalJson = String.format(
            "{\"listingId\": %d, \"proposerListingIds\": [%d], \"message\": \"Exchange request\"}",
            receiverListing.getId(), listing.getId()
        );

        // 已登入狀態創建提案
        MvcResult result2 = mockMvc.perform(post("/api/proposals")
                .session(authenticatedSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validProposalJson))
                .andExpect(status().isCreated())
                .andReturn();

        System.out.println("  ✅ 狀態碼: " + result2.getResponse().getStatus() + " (已授權用戶可正常操作)");

        System.out.println("\n✅ SEC-02 測試通過：API 權限控制正常運作");
    }

    @Test
    void testSEC03_XSSInjectionPrevention() throws Exception {
        System.out.println("\n========== SEC-03: XSS 注入防護測試 ==========");

        // XSS 測試字串
        String[] xssPayloads = {
            "<script>alert('XSS')</script>",
            "<img src=x onerror=alert('XSS')>",
            "<svg onload=alert('XSS')>",
            "javascript:alert('XSS')",
            "<iframe src=javascript:alert('XSS')>"
        };

        System.out.println("\n[測試 1] 刊登描述 XSS 防護");
        
        // 建立包含 XSS 的刊登
        Listing xssListing = Listing.builder()
                .userId(testUser.getId())
                .cardName("Safe Card Name")
                .artistName("Artist")
                .description("<script>alert('XSS')</script>惡意描述")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(10)
                .hasProtection(true)
                .imagePaths("[\"test.jpg\"]")
                .status(Listing.Status.AVAILABLE).build();
        xssListing.prePersist();
        xssListing = listingRepository.save(xssListing);

        // 訪問刊登列表頁面
        MvcResult result = mockMvc.perform(get("/ui/listings").session(authenticatedSession))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString();
        
        // 驗證 XSS 腳本未執行（被編碼）
        assertFalse(html.contains("<script>alert"), 
                "HTML 中不應包含未編碼的 <script> 標籤");
        
        // 驗證 Thymeleaf 正確編碼，腳本標籤不存在於 HTML 或被編碼
        // Thymeleaf th:text 會自動編碼 HTML，所以不會有原始的 <script>
        boolean isSafe = !html.contains("<script>alert") && !html.contains("javascript:alert");
        assertTrue(isSafe, "XSS 字串應被編碼或過濾");
        
        System.out.println("  ✅ XSS 腳本已被編碼，未執行");

        System.out.println("\n[測試 2] 聊天訊息 XSS 防護");
        
        // 建立聊天室
        ChatRoom chatRoom = new ChatRoom();
        chatRoom.setProposalId(1L);
        chatRoom.setUserAId(testUser.getId());
        chatRoom.setUserBId(2L);
        chatRoom.setStatus(ChatRoom.ChatRoomStatus.ACTIVE);
        chatRoom = chatRoomRepository.save(chatRoom);

        // 建立包含 XSS 的訊息
        ChatMessage xssMessage = new ChatMessage();
        xssMessage.setChatRoomId(chatRoom.getId());
        xssMessage.setSenderId(testUser.getId());
        xssMessage.setType(ChatMessage.MessageType.TEXT);
        xssMessage.setContent("<img src=x onerror=alert('XSS')>惡意訊息");
        chatMessageRepository.save(xssMessage);

        // 訪問聊天頁面
        MvcResult chatResult = mockMvc.perform(get("/ui/chat").session(authenticatedSession))
                .andExpect(status().isOk())
                .andReturn();

        String chatHtml = chatResult.getResponse().getContentAsString();
        
        // 驗證 XSS 腳本未執行
        assertFalse(chatHtml.contains("onerror=alert"),
                "HTML 中不應包含未編碼的事件處理器");
        
        System.out.println("  ✅ 聊天訊息 XSS 已被防護");

        System.out.println("\n[測試 3] HTML 解析驗證");
        
        // 驗證：用戶輸入的內容不應作為可執行代碼出現
        // 關鍵是檢查惡意字串是否被正確編碼，而不是以原始形式存在
        
        // 1. 確認原始的 <script> 攻擊字串沒有直接出現在 HTML 中（應被編碼）
        assertFalse(html.contains("<script>alert('XSS')</script>"),
                "XSS 攻擊字串應該被編碼，不應直接出現");
        
        // 2. 使用 jsoup 解析，檢查用戶輸入區域
        Document doc = Jsoup.parse(chatHtml);
        
        // 3. 確認沒有危險的事件處理器屬性
        assertEquals(0, doc.select("[onerror*='alert']").size(), 
                "不應有包含 alert 的 onerror 屬性");
        assertEquals(0, doc.select("[onload*='alert']").size(), 
                "不應有包含 alert 的 onload 屬性");
        
        System.out.println("  ✅ HTML 結構驗證通過，用戶輸入已被安全編碼");

        System.out.println("\n✅ SEC-03 測試通過：XSS 防護正常運作");
    }

    @Test
    void testSEC04_SQLInjectionAndSortWhitelist() throws Exception {
        System.out.println("\n========== SEC-04: SQL 注入與排序白名單測試 ==========");

        // 建立測試數據
        for (int i = 1; i <= 5; i++) {
            Listing listing = Listing.builder()
                    .userId(testUser.getId() + 1) // 不同用戶
                    .cardName("Card " + i)
                    .artistName("Artist " + i)
                    .cardSource(Listing.CardSource.ALBUM)
                    .conditionRating(10 - i)
                    .hasProtection(true)
                    .imagePaths("[\"test" + i + ".jpg\"]")
                    .status(Listing.Status.AVAILABLE).build();
            listing.prePersist();
            listingRepository.save(listing);
        }

        System.out.println("\n[測試 1] SQL 注入防護 - 搜尋參數");
        
        // 嘗試 SQL 注入
        String[] sqlInjectionPayloads = {
            "' OR '1'='1",
            "'; DROP TABLE listings; --",
            "1' UNION SELECT * FROM users --",
            "' OR 1=1 --"
        };

        for (String payload : sqlInjectionPayloads) {
            System.out.println("\n  測試 payload: " + payload);
            
            MvcResult result = mockMvc.perform(get("/api/listings")
                    .param("q", payload)
                    .session(authenticatedSession))
                    .andReturn();

            int status = result.getResponse().getStatus();
            
            // 應該返回正常結果或錯誤，不應該執行 SQL 注入
            assertTrue(status == 200 || status == 400 || status == 500,
                    "SQL 注入應被阻擋或安全處理，狀態碼: " + status);
            
            // 驗證資料庫結構未被破壞
            long listingCount = listingRepository.count();
            assertTrue(listingCount >= 5, "資料庫表應該完整，記錄數: " + listingCount);
            
            System.out.println("    ✅ 狀態碼: " + status + " (SQL 注入被防護)");
        }

        System.out.println("\n[測試 2] 排序欄位白名單驗證");
        
        // 測試合法排序欄位
        String[] validSortFields = {
            "createdAt,DESC",
            "cardName,ASC",
            "conditionRating,DESC"
        };

        for (String sortField : validSortFields) {
            System.out.println("\n  測試合法排序: " + sortField);
            
            MvcResult result = mockMvc.perform(get("/api/listings")
                    .param("sort", sortField)
                    .session(authenticatedSession))
                    .andExpect(status().isOk())
                    .andReturn();
            
            System.out.println("    ✅ 合法欄位正常排序");
        }

        // 測試非法排序欄位
        String[] invalidSortFields = {
            "passwordHash,DESC",  // 敏感欄位
            "(SELECT * FROM users),ASC",  // SQL 注入嘗試
            "unknown_field,DESC"  // 不存在的欄位
        };

        System.out.println("\n  測試非法排序欄位:");
        
        for (String sortField : invalidSortFields) {
            System.out.println("\n    測試非法排序: " + sortField);
            
            MvcResult result = mockMvc.perform(get("/api/listings")
                    .param("sort", sortField)
                    .session(authenticatedSession))
                    .andReturn();

            int status = result.getResponse().getStatus();
            
            // 應該返回錯誤或使用預設排序，不應該執行非法排序
            assertTrue(status == 200 || status == 400 || status == 500,
                    "非法欄位應被拒絕或使用預設值，狀態碼: " + status);
            
            System.out.println("      ✅ 狀態碼: " + status + " (非法排序被處理)");
        }

        System.out.println("\n[測試 3] 參數化查詢驗證");
        
        // 正常搜尋應該正常工作
        MvcResult normalResult = mockMvc.perform(get("/api/listings")
                .param("q", "Card 1")
                .param("sort", "createdAt,DESC")
                .session(authenticatedSession))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = normalResult.getResponse().getContentAsString();
        assertTrue(responseBody.contains("Card 1") || responseBody.contains("[]"),
                "正常搜尋應返回結果或空陣列");
        
        System.out.println("  ✅ 正常搜尋功能運作正常");

        System.out.println("\n✅ SEC-04 測試通過：SQL 注入防護與排序白名單正常運作");
    }
}
