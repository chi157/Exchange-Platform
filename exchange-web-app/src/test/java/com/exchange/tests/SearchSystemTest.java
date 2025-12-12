package com.exchange.tests;

import com.exchange.platform.ExchangeWebAppApplication;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 搜尋與篩選系統測試類別（Search and Filter System Test）
 * 
 * <p>本測試類別覆蓋「模組 C：搜尋與篩選」的所有功能測試案例（TC-SR01~TC-SR04），
 * 驗證刊登搜尋功能的查詢（關鍵字搜尋）、分頁處理、排序功能，以及邊界條件與異常輸入的處理。
 * 
 * <h2>測試範圍與前置條件</h2>
 * <ul>
 *   <li><b>測試範圍：</b>刊登列表查詢、關鍵字搜尋、分頁參數驗證、排序參數驗證、無結果處理</li>
 *   <li><b>前置條件：</b>
 *     <ol>
 *       <li>使用者已成功登入系統（透過 MockHttpSession 模擬）</li>
 *       <li>資料庫中已建立多筆測試刊登資料（至少 50 筆，涵蓋不同分類與關鍵字）</li>
 *       <li>測試前清空資料，確保測試隔離性</li>
 *     </ol>
 *   </li>
 * </ul>
 * 
 * <h2>API 端點與參數</h2>
 * <ul>
 *   <li><b>GET /api/listings</b> - 刊登列表查詢（支援分頁、搜尋、排序）</li>
 *   <li><b>查詢參數：</b>
 *     <ul>
 *       <li>q（查詢關鍵字）：支援搜尋 cardName、artistName、groupName、description 欄位</li>
 *       <li>page（頁碼）：1-based 頁碼，預設第 1 頁</li>
 *       <li>size（每頁筆數）：預設 5 筆，最大 100 筆</li>
 *       <li>sort（排序）：格式 "欄位,方向"，例如 "createdAt,desc"，預設依建立時間降序</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h2>搜尋實作細節</h2>
 * <ul>
 *   <li><b>關鍵字搜尋：</b>使用 LIKE 查詢，不區分大小寫，支援部分匹配</li>
 *   <li><b>搜尋範圍：</b>cardName OR artistName OR groupName OR description</li>
 *   <li><b>分頁處理：</b>
 *     <ul>
 *       <li>page  0 或 null  預設第 1 頁（轉換為 0-based index 0）</li>
 *       <li>size  0 或 null  預設 5 筆</li>
 *       <li>size > 100  限制最大 100 筆</li>
 *     </ul>
 *   </li>
 *   <li><b>排序處理：</b>
 *     <ul>
 *       <li>格式："屬性名稱,方向"（例如 "createdAt,desc"）</li>
 *       <li>預設排序：createdAt DESC（最新的在前）</li>
 *       <li>支援欄位：createdAt, cardName, conditionRating 等 Listing 實體欄位</li>
 *       <li>非法排序參數處理：若輸入非法參數（如 SQL 注入嘗試），系統應拒絕或採用預設排序</li>
 *     </ul>
 *   </li>
 *   <li><b>回應格式：</b>直接回傳 JSON 陣列（非分頁物件），包含 ListingDTO 列表</li>
 * </ul>
 * 
 * <h2>測試策略</h2>
 * <ul>
 *   <li><b>正向測試：</b>驗證合法查詢參數的成功路徑（TC-SR01）</li>
 *   <li><b>邊界測試：</b>驗證分頁參數邊界條件（TC-SR02：負數頁碼、超大 size）</li>
 *   <li><b>安全測試：</b>驗證排序參數安全性，防止 SQL 注入（TC-SR03）</li>
 *   <li><b>空結果測試：</b>驗證無匹配結果時的正常回應（TC-SR04）</li>
 * </ul>
 * 
 * <h2>測試數據準備策略</h2>
 * <ul>
 *   <li>建立 50+ 筆測試刊登，涵蓋多種卡片來源（ALBUM/CONCERT/EVENT_CARD 等）</li>
 *   <li>使用不同的 cardName、artistName、groupName 確保搜尋覆蓋率</li>
 *   <li>設定不同的 createdAt 時間戳，確保排序測試可驗證</li>
 *   <li>包含特殊關鍵字（如 "Special Card"）用於正向搜尋測試</li>
 *   <li>準備罕見關鍵字（如 "NONEXISTENT_KEYWORD_XYZ"）用於無結果測試</li>
 * </ul>
 * 
 * @author 陳欣妤（測試工程師）
 * @version 1.0
 * @since 2025-12-12
 */
@SpringBootTest(classes = ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SearchSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private MockHttpSession userSession;

    private final String TEST_USER_EMAIL = "searchtest@example.com";

    @BeforeEach
    void setUp() {
        // 清空資料庫
        listingRepository.deleteAll();
        userRepository.deleteAll();

        // 建立測試使用者
        testUser = User.builder()
                .email(TEST_USER_EMAIL)
                .passwordHash("password123")
                .displayName("Search Test User")
                .verified(true)
                .roles("USER")
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        // 建立會話
        userSession = new MockHttpSession();
        userSession.setAttribute("userId", testUser.getId());

        // 準備測試資料：建立 50+ 筆刊登，涵蓋多種類型
        createTestListings();
    }

    /**
     * 建立測試用刊登資料
     * - 共 55 筆刊登
     * - 涵蓋不同的 cardSource（ALBUM/CONCERT/EVENT_CARD/FAN_MEETING/SPECIAL_CARD）
     * - 不同的關鍵字組合（用於搜尋測試）
     * - 不同的時間戳（用於排序測試）
     */
    private void createTestListings() {
        Listing.CardSource[] sources = {
            Listing.CardSource.ALBUM,
            Listing.CardSource.CONCERT,
            Listing.CardSource.EVENT_CARD,
            Listing.CardSource.FAN_MEETING,
            Listing.CardSource.SPECIAL_CARD
        };

        // 建立 55 筆測試資料
        for (int i = 1; i <= 55; i++) {
            Listing.CardSource source = sources[(i - 1) % sources.length];
            
            // 每 10 筆使用特殊關鍵字 "Special Card" 用於搜尋測試
            String cardName = (i % 10 == 0) 
                ? "Special Card " + i 
                : "Card " + i;
            
            String artistName = "Artist " + ((i % 5) + 1); // Artist 1-5 循環
            String groupName = "Group " + ((i % 3) + 1);   // Group 1-3 循環
            
            Listing listing = Listing.builder()
                    .cardName(cardName)
                    .artistName(artistName)
                    .groupName(groupName)
                    .description("Description for card " + i)
                    .cardSource(source)
                    .conditionRating(5 + (i % 6)) // 5-10 品相等級
                    .hasProtection(i % 2 == 0)
                    .imagePaths("[\"image" + i + ".jpg\"]")
                    .userId(testUser.getId())
                    .status(Listing.Status.AVAILABLE)
                    .createdAt(LocalDateTime.now().minusDays(55 - i)) // 越後面建立的越新
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            listingRepository.save(listing);
        }
    }

    // ==================== TC-SR01: 成功搜尋 ====================
    /**
     * TC-SR01: 成功搜尋
     * Given: 資料庫有 50+ 筆刊登，包含特定關鍵字 "Special Card"
     * When: GET /api/listings?q=Special&page=0&size=10
     * Then: 回傳 200 OK，結果包含所有匹配 "Special" 的刊登，分頁正確
     */
    @Test
    @DisplayName("TC-SR01: 成功搜尋")
    void testSuccessfulSearch() throws Exception {
        // Given: 資料庫已有 55 筆刊登，其中包含 "Special Card" 關鍵字的刊登

        // When & Then: 搜尋 "Special" 關鍵字
        mockMvc.perform(get("/api/listings")
                        .param("q", "Special")
                        .param("page", "0")
                        .param("size", "10")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[*].cardName").value(hasItem(containsString("Special"))))
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));

        // When & Then: 搜尋 "Artist 2" 應該回傳多筆結果
        mockMvc.perform(get("/api/listings")
                        .param("q", "Artist 2")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$[*].artistName").value(everyItem(containsString("Artist 2"))));

        // When & Then: 不區分大小寫搜尋
        mockMvc.perform(get("/api/listings")
                        .param("q", "special")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));

        System.out.println(" TC-SR01 通過：成功搜尋，回傳 200，結果正確過濾");
    }

    // ==================== TC-SR02: 分頁邊界測試 ====================
    /**
     * TC-SR02: 分頁邊界測試
     * Given: 資料庫有 55 筆刊登
     * When: 使用非法分頁參數（page=-1, size=1000）
     * Then: 系統採用預設值或限制最大值，不拋出錯誤，回傳 200 OK
     */
    @Test
    @DisplayName("TC-SR02: 分頁邊界測試")
    void testPaginationBoundary() throws Exception {
        // Given: 資料庫有 55 筆刊登

        // When & Then: 負數頁碼應採用預設（第 1 頁）
        mockMvc.perform(get("/api/listings")
                        .param("page", "-1")
                        .param("size", "5")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(5)));

        // When & Then: 超大 size 應限制為最大值（100）
        mockMvc.perform(get("/api/listings")
                        .param("page", "0")
                        .param("size", "1000")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(100)));

        // When & Then: size=0 應採用預設值（5）
        mockMvc.perform(get("/api/listings")
                        .param("page", "0")
                        .param("size", "0")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(lessThanOrEqualTo(5)));

        // When & Then: 不提供分頁參數應使用預設值
        mockMvc.perform(get("/api/listings")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));

        System.out.println(" TC-SR02 通過：分頁邊界處理正確，系統採用預設值或限制最大值");
    }

    // ==================== TC-SR03: 非法排序參數（安全性測試） ====================
    /**
     * TC-SR03: 非法排序參數（安全性測試）
     * Given: 資料庫有多筆刊登
     * When: 使用非法排序參數（如 SQL 注入嘗試 "DROP TABLE"）
     * Then: 系統拒絕或採用預設排序，不執行惡意操作，回傳 200 OK 或 400 Bad Request
     */
    @Test
    @DisplayName("TC-SR03: 非法排序參數（安全性測試）")
    void testInvalidSortParameter() throws Exception {
        // Given: 資料庫有 55 筆刊登

        // When & Then: SQL 注入嘗試 - 系統應拒絕或採用預設排序，不應崩潰
        mockMvc.perform(get("/api/listings")
                        .param("sort", "DROP TABLE")
                        .session(userSession))
                .andExpect(status().isOk()) // 系統應正常回應，不崩潰
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThan(0)));

        // When & Then: 非法排序欄位
        mockMvc.perform(get("/api/listings")
                        .param("sort", "nonExistentField,desc")
                        .session(userSession))
                .andExpect(status().isOk()) // 應採用預設排序
                .andExpect(jsonPath("$").isArray());

        // When & Then: 非法排序方向
        mockMvc.perform(get("/api/listings")
                        .param("sort", "createdAt,INVALID_DIRECTION")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // When & Then: 包含特殊字符的排序參數
        mockMvc.perform(get("/api/listings")
                        .param("sort", "createdAt; DROP TABLE listings; --")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        System.out.println(" TC-SR03 通過：非法排序參數被正確處理，系統未執行惡意操作");
    }

    // ==================== TC-SR04: 無結果測試 ====================
    /**
     * TC-SR04: 無結果測試
     * Given: 資料庫有多筆刊登
     * When: 使用罕見關鍵字搜尋（不存在於任何刊登中）
     * Then: 回傳 200 OK，結果為空陣列（不是錯誤）
     */
    @Test
    @DisplayName("TC-SR04: 無結果測試")
    void testNoResults() throws Exception {
        // Given: 資料庫有 55 筆刊登，但沒有包含特定罕見關鍵字

        // When & Then: 搜尋不存在的關鍵字
        mockMvc.perform(get("/api/listings")
                        .param("q", "NONEXISTENT_KEYWORD_XYZ_12345")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        // When & Then: 搜尋另一個不存在的關鍵字
        mockMvc.perform(get("/api/listings")
                        .param("q", "ThisCardDoesNotExist")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        System.out.println(" TC-SR04 通過：無結果時回傳 200 OK，空陣列");
    }

    // ==================== 輔助測試：排序功能驗證 ====================
    /**
     * 輔助測試：排序功能驗證
     * Given: 資料庫有多筆刊登，具有不同的 createdAt 時間戳
     * When: 使用不同的排序參數（createdAt desc/asc）
     * Then: 結果按指定順序排列
     */
    @Test
    @DisplayName("輔助測試：排序功能驗證")
    void testSortingFunctionality() throws Exception {
        // Given: 資料庫有 55 筆刊登，createdAt 從舊到新

        // When & Then: 預設排序（createdAt desc）- 最新的在前
        String response1 = mockMvc.perform(get("/api/listings")
                        .param("size", "5")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardName").value(containsString("55"))) // 最新建立的
                .andReturn().getResponse().getContentAsString();

        // When & Then: 明確指定 createdAt,desc
        mockMvc.perform(get("/api/listings")
                        .param("sort", "createdAt,desc")
                        .param("size", "5")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardName").value(containsString("55")));

        // When & Then: 指定 createdAt,asc - 最舊的在前
        mockMvc.perform(get("/api/listings")
                        .param("sort", "createdAt,asc")
                        .param("size", "5")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardName").value(containsString("1"))); // 最早建立的

        System.out.println(" 輔助測試通過：排序功能正常運作");
    }

    // ==================== 輔助測試：未登入存取 ====================
    /**
     * 輔助測試：未登入存取
     * Given: 無會話（未登入）
     * When: GET /api/listings
     * Then: 根據實作，可能允許訪問（200）或要求登入（401）
     */
    @Test
    @DisplayName("輔助測試：未登入存取搜尋")
    void testUnauthenticatedAccess() throws Exception {
        // When & Then: 未登入訪問列表
        // 根據實作，搜尋功能可能允許公開訪問
        mockMvc.perform(get("/api/listings")
                        .param("q", "Card"))
                .andExpect(status().isOk()) // 假設允許公開搜尋
                .andExpect(jsonPath("$").isArray());

        System.out.println(" 輔助測試通過：未登入存取處理正確");
    }
}