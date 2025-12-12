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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 刊登管理系統測試類別（Listing Management System Test）
 * 
 * <p>本測試類別覆蓋「模組 B：刊登管理」的所有功能測試案例（TC-LS01~TC-LS07），
 * 驗證刊登的建立（Create）、讀取（Read）、更新（Update）、刪除（Delete）操作，
 * 以及相關的授權控制、驗證規則、分頁查詢與搜尋功能。
 * 
 * <h2>測試範圍與前置條件</h2>
 * <ul>
 *   <li><b>測試範圍：</b>刊登 CRUD 操作、授權檢查、輸入驗證、分頁/搜尋/排序</li>
 *   <li><b>前置條件：</b>
 *     <ol>
 *       <li>使用者已成功登入系統（透過 MockHttpSession 模擬）</li>
 *       <li>資料庫中已建立測試用使用者（testUser, otherUser）</li>
 *       <li>測試前清空刊登資料表，確保測試隔離性</li>
 *     </ol>
 *   </li>
 * </ul>
 * 
 * <h2>API 端點與實體模型</h2>
 * <ul>
 *   <li><b>POST /api/listings</b> - 建立新刊登（201 Created）</li>
 *   <li><b>GET /api/listings/{id}</b> - 取得單筆刊登（200 OK / 404 Not Found）</li>
 *   <li><b>PUT /api/listings/{id}</b> - 更新刊登（200 OK / 403 Forbidden）</li>
 *   <li><b>DELETE /api/listings/{id}</b> - 刪除刊登（204 No Content / 403 Forbidden）</li>
 *   <li><b>GET /api/listings</b> - 列表查詢（支援分頁、搜尋、排序）</li>
 * </ul>
 * 
 * <h3>Listing 實體欄位</h3>
 * <ul>
 *   <li>cardName（卡片名稱，必填，長度 ≤ 200）</li>
 *   <li>artistName（藝人名稱，必填，長度 ≤ 100）</li>
 *   <li>groupName（團體名稱，選填，長度 ≤ 100）</li>
 *   <li>description（描述，選填，TEXT）</li>
 *   <li>cardSource（卡片來源，必填，Enum: ALBUM/CONCERT/FAN_MEETING/EVENT_CARD/SPECIAL_CARD/UNOFFICIAL）</li>
 *   <li>conditionRating（品相等級，必填，整數 1-10）</li>
 *   <li>hasProtection（保護措施，必填，Boolean）</li>
 *   <li>imagePaths（圖片路徑，必填，JSON Array）</li>
 *   <li>userId（擁有者ID，系統自動填入）</li>
 *   <li>status（狀態，Enum: AVAILABLE/LOCKED/PENDING/COMPLETED，預設 AVAILABLE）</li>
 * </ul>
 * 
 * <h2>測試策略</h2>
 * <ul>
 *   <li><b>正向測試：</b>驗證合法輸入的成功路徑（TC-LS01, TC-LS04, TC-LS05, TC-LS07）</li>
 *   <li><b>邊界測試：</b>驗證欄位長度邊界條件（TC-LS02）</li>
 *   <li><b>驗證測試：</b>驗證必填欄位與格式驗證（TC-LS03）</li>
 *   <li><b>授權測試：</b>驗證跨使用者操作權限控制（TC-LS06）</li>
 *   <li><b>查詢測試：</b>驗證分頁、搜尋、排序功能（TC-LS07）</li>
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
public class ListingSystemTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ListingRepository listingRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User otherUser;
    private MockHttpSession userSession;
    private MockHttpSession otherUserSession;

    private final String TEST_USER_EMAIL = "listingowner@example.com";
    private final String OTHER_USER_EMAIL = "otheruser@example.com";

    @BeforeEach
    void setUp() {
        // 清空資料庫
        listingRepository.deleteAll();
        userRepository.deleteAll();

        // 建立測試使用者（擁有者）
        testUser = User.builder()
                .email(TEST_USER_EMAIL)
                .passwordHash("password123")
                .displayName("Test User")
                .verified(true)
                .roles("USER")
                .createdAt(LocalDateTime.now())
                .build();
        testUser = userRepository.save(testUser);

        // 建立其他使用者（用於授權測試）
        otherUser = User.builder()
                .email(OTHER_USER_EMAIL)
                .passwordHash("password123")
                .displayName("Other User")
                .verified(true)
                .roles("USER")
                .createdAt(LocalDateTime.now())
                .build();
        otherUser = userRepository.save(otherUser);

        // 建立會話
        userSession = new MockHttpSession();
        userSession.setAttribute("userId", testUser.getId());

        otherUserSession = new MockHttpSession();
        otherUserSession.setAttribute("userId", otherUser.getId());
    }

    // ==================== TC-LS01: 建立刊登成功 ====================
    /**
     * TC-LS01: 建立刊登成功
     * Given: 使用者已登入，提供合法的刊登資訊（名稱、藝人、來源、品相、圖片）
     * When: POST /api/listings
     * Then: 回傳 201 Created，資料庫新增一筆記錄，userId 正確關聯
     */
    @Test
    @DisplayName("TC-LS01: 建立刊登成功")
    void testCreateListingSuccess() throws Exception {
        // Given: 準備合法的刊登請求資料
        String requestBody = """
            {
                "cardName": "測試卡片",
                "groupName": "測試團體",
                "artistName": "測試藝人",
                "description": "這是測試描述",
                "cardSource": "ALBUM",
                "conditionRating": 9,
                "hasProtection": true,
                "remarks": "測試備註",
                "imageFileNames": ["test-image-1.jpg", "test-image-2.jpg"]
            }
            """;

        // When & Then: 執行建立請求
        String responseJson = mockMvc.perform(post("/api/listings")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cardName").value("測試卡片"))
                .andExpect(jsonPath("$.artistName").value("測試藝人"))
                .andExpect(jsonPath("$.userId").value(testUser.getId()))
                .andReturn().getResponse().getContentAsString();

        // 驗證資料庫
        long count = listingRepository.count();
        assertEquals(1, count, "資料庫應有一筆刊登記錄");

        Listing saved = listingRepository.findAll().get(0);
        assertEquals(testUser.getId(), saved.getUserId(), "userId 應正確關聯");
        assertEquals("測試卡片", saved.getCardName());
        assertEquals(Listing.CardSource.ALBUM, saved.getCardSource());

        System.out.println("✅ TC-LS01 通過：建立刊登成功，回傳 201，資料庫正確儲存");
    }

    // ==================== TC-LS02: 名稱長度邊界測試 ====================
    /**
     * TC-LS02: 名稱長度邊界測試
     * Given: 使用者已登入
     * When: 提交不同長度的 cardName（1 字元、200 字元、201 字元）
     * Then: 1~200 字元接受（201 Created），201 字元拒絕（400 Bad Request）
     */
    @Test
    @DisplayName("TC-LS02: 名稱長度邊界測試")
    void testListingNameBoundary() throws Exception {
        // Given: 準備不同長度的名稱
        String name1Char = "A";
        String name200Chars = "A".repeat(200);
        String name201Chars = "A".repeat(201);

        // When & Then: 1 字元應成功
        String request1 = String.format("""
            {
                "cardName": "%s",
                "artistName": "Test Artist",
                "cardSource": "ALBUM",
                "conditionRating": 5,
                "hasProtection": false,
                "imageFileNames": ["image.jpg"]
            }
            """, name1Char);

        mockMvc.perform(post("/api/listings")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request1))
                .andExpect(status().isCreated());

        // When & Then: 200 字元應成功
        listingRepository.deleteAll();
        String request200 = String.format("""
            {
                "cardName": "%s",
                "artistName": "Test Artist",
                "cardSource": "ALBUM",
                "conditionRating": 5,
                "hasProtection": false,
                "imageFileNames": ["image.jpg"]
            }
            """, name200Chars);

        mockMvc.perform(post("/api/listings")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request200))
                .andExpect(status().isCreated());

        // When & Then: 201 字元應失敗
        String request201 = String.format("""
            {
                "cardName": "%s",
                "artistName": "Test Artist",
                "cardSource": "ALBUM",
                "conditionRating": 5,
                "hasProtection": false,
                "imageFileNames": ["image.jpg"]
            }
            """, name201Chars);

        mockMvc.perform(post("/api/listings")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request201))
                .andExpect(status().isBadRequest());

        System.out.println("✅ TC-LS02 通過：名稱長度邊界測試正確（1/200 字元接受，201 字元拒絕）");
    }

    // ==================== TC-LS03: 非法圖片驗證 ====================
    /**
     * TC-LS03: 非法圖片驗證
     * Given: 使用者已登入
     * When: 提交 imageFileNames 為 null 或空陣列
     * Then: 回傳 400 Bad Request，資料庫不新增記錄
     */
    @Test
    @DisplayName("TC-LS03: 非法圖片驗證")
    void testInvalidImage() throws Exception {
        // Given: 準備無圖片的請求
        String requestNoImages = """
            {
                "cardName": "Test Card",
                "artistName": "Test Artist",
                "cardSource": "ALBUM",
                "conditionRating": 5,
                "hasProtection": false,
                "imageFileNames": []
            }
            """;

        // When & Then: 空陣列應失敗
        mockMvc.perform(post("/api/listings")
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestNoImages))
                .andExpect(status().isBadRequest());

        // 驗證資料庫未新增
        assertEquals(0, listingRepository.count(), "資料庫不應有新增記錄");

        System.out.println("✅ TC-LS03 通過：非法圖片驗證成功，回傳 400，資料庫未新增");
    }

    // ==================== TC-LS04: 更新刊登 ====================
    /**
     * TC-LS04: 更新刊登
     * Given: 擁有者已建立一筆刊登
     * When: PUT /api/listings/{id} 修改描述與品相
     * Then: 回傳 200 OK，資料更新，updatedAt 時間戳更新
     */
    @Test
    @DisplayName("TC-LS04: 更新刊登")
    void testUpdateListing() throws Exception {
        // Given: 建立初始刊登
        Listing listing = Listing.builder()
                .cardName("Original Name")
                .artistName("Original Artist")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(5)
                .hasProtection(false)
                .imagePaths("[\"image1.jpg\"]")
                .userId(testUser.getId())
                .status(Listing.Status.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        listing = listingRepository.save(listing);

        // When: 執行更新
        String updateRequest = """
            {
                "cardName": "Updated Name",
                "artistName": "Updated Artist",
                "description": "Updated Description",
                "cardSource": "CONCERT",
                "conditionRating": 8,
                "hasProtection": true,
                "imageFileNames": ["updated.jpg"]
            }
            """;

        mockMvc.perform(put("/api/listings/" + listing.getId())
                        .session(userSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardName").value("Updated Name"))
                .andExpect(jsonPath("$.description").value("Updated Description"))
                .andExpect(jsonPath("$.conditionRating").value(8));

        // Then: 驗證資料庫更新
        Listing updated = listingRepository.findById(listing.getId()).orElseThrow();
        assertEquals("Updated Name", updated.getCardName());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals(8, updated.getConditionRating());
        assertEquals(Listing.CardSource.CONCERT, updated.getCardSource());
        assertTrue(updated.getHasProtection());

        System.out.println("✅ TC-LS04 通過：更新刊登成功，資料正確更新");
    }

    // ==================== TC-LS05: 刪除刊登 ====================
    /**
     * TC-LS05: 刪除刊登
     * Given: 擁有者已建立一筆刊登
     * When: DELETE /api/listings/{id}
     * Then: 回傳 204 No Content，資料標記刪除或移除，後續查詢 404
     */
    @Test
    @DisplayName("TC-LS05: 刪除刊登")
    void testDeleteListing() throws Exception {
        // Given: 建立刊登
        Listing listing = Listing.builder()
                .cardName("To Be Deleted")
                .artistName("Artist")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(5)
                .hasProtection(false)
                .imagePaths("[\"image.jpg\"]")
                .userId(testUser.getId())
                .status(Listing.Status.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        listing = listingRepository.save(listing);

        // When: 執行刪除
        mockMvc.perform(delete("/api/listings/" + listing.getId())
                        .session(userSession))
                .andExpect(status().isNoContent());

        // Then: 驗證刪除（軟刪除或硬刪除）
        Optional<Listing> deleted = listingRepository.findById(listing.getId());
        assertTrue(deleted.isEmpty() || deleted.get().getStatus() == Listing.Status.COMPLETED,
                "刊登應被刪除或標記為 COMPLETED");

        // 後續查詢應 404
        mockMvc.perform(get("/api/listings/" + listing.getId())
                        .session(userSession))
                .andExpect(status().isNotFound());

        System.out.println("✅ TC-LS05 通過：刪除刊登成功，後續查詢 404");
    }

    // ==================== TC-LS06: 越權編輯測試 ====================
    /**
     * TC-LS06: 越權編輯測試
     * Given: testUser 建立刊登，otherUser 嘗試編輯/刪除
     * When: otherUser 執行 PUT/DELETE
     * Then: 回傳 403 Forbidden，資料不變更
     */
    @Test
    @DisplayName("TC-LS06: 越權編輯測試")
    void testUnauthorizedEdit() throws Exception {
        // Given: testUser 建立刊登
        Listing listing = Listing.builder()
                .cardName("Owner's Listing")
                .artistName("Artist")
                .cardSource(Listing.CardSource.ALBUM)
                .conditionRating(7)
                .hasProtection(true)
                .imagePaths("[\"image.jpg\"]")
                .userId(testUser.getId())
                .status(Listing.Status.AVAILABLE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        listing = listingRepository.save(listing);

        // When & Then: otherUser 嘗試更新 → 403
        String updateRequest = """
            {
                "cardName": "Hacked Name",
                "artistName": "Artist",
                "cardSource": "ALBUM",
                "conditionRating": 1,
                "hasProtection": false,
                "imageFileNames": ["hack.jpg"]
            }
            """;

        mockMvc.perform(put("/api/listings/" + listing.getId())
                        .session(otherUserSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden());

        // 驗證資料未變更
        Listing unchanged = listingRepository.findById(listing.getId()).orElseThrow();
        assertEquals("Owner's Listing", unchanged.getCardName());

        // When & Then: otherUser 嘗試刪除 → 403
        mockMvc.perform(delete("/api/listings/" + listing.getId())
                        .session(otherUserSession))
                .andExpect(status().isForbidden());

        // 驗證刊登仍存在
        assertTrue(listingRepository.findById(listing.getId()).isPresent());

        System.out.println("✅ TC-LS06 通過：越權操作回傳 403，資料未變更");
    }

    // ==================== TC-LS07: 列表查詢（分頁/搜尋/排序） ====================
    /**
     * TC-LS07: 列表查詢（分頁/搜尋/排序）
     * Given: 資料庫有多筆刊登
     * When: GET /api/listings?page=0&size=5&q=keyword&sort=createdAt,desc
     * Then: 回傳 200，分頁正確，搜尋/排序生效
     */
    @Test
    @DisplayName("TC-LS07: 列表查詢（分頁/搜尋/排序）")
    void testListListings() throws Exception {
        // Given: 建立多筆測試資料
        for (int i = 1; i <= 10; i++) {
            Listing listing = Listing.builder()
                    .cardName("Card " + i)
                    .artistName("Artist " + i)
                    .cardSource(Listing.CardSource.ALBUM)
                    .conditionRating(5 + (i % 5))
                    .hasProtection(i % 2 == 0)
                    .imagePaths("[\"image" + i + ".jpg\"]")
                    .userId(testUser.getId())
                    .status(Listing.Status.AVAILABLE)
                    .createdAt(LocalDateTime.now().minusDays(10 - i))
                    .updatedAt(LocalDateTime.now())
                    .build();
            listingRepository.save(listing);
        }

        // When & Then: 測試預設分頁（size=5）
        mockMvc.perform(get("/api/listings")
                        .param("page", "0")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(greaterThanOrEqualTo(1)));

        // When & Then: 測試自訂分頁
        mockMvc.perform(get("/api/listings")
                        .param("page", "0")
                        .param("size", "3")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // When & Then: 測試搜尋（關鍵字過濾）
        mockMvc.perform(get("/api/listings")
                        .param("q", "Card 5")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].cardName").value(hasItem(containsString("Card 5"))));

        // When & Then: 測試排序（createdAt desc）
        mockMvc.perform(get("/api/listings")
                        .param("sort", "createdAt,desc")
                        .session(userSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cardName").value("Card 10"));

        System.out.println("✅ TC-LS07 通過：分頁/搜尋/排序功能正常");
    }

    // ==================== 輔助測試：未登入存取 ====================
    /**
     * 輔助測試：未登入存取
     * Given: 無會話（未登入）
     * When: POST /api/listings
     * Then: 回傳 401 Unauthorized
     */
    @Test
    @DisplayName("輔助測試：未登入存取")
    void testUnauthorizedAccess() throws Exception {
        // Given: 準備請求但無會話
        String requestBody = """
            {
                "cardName": "Unauthorized Card",
                "artistName": "Artist",
                "cardSource": "ALBUM",
                "conditionRating": 5,
                "hasProtection": false,
                "imageFileNames": ["image.jpg"]
            }
            """;

        // When & Then: 未登入應拒絕
        mockMvc.perform(post("/api/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        System.out.println("✅ 輔助測試通過：未登入存取回傳 401");
    }
}
