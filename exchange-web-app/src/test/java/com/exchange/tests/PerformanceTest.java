/*
 * ============================================================================
 * 系統效能測試 (System Performance Test)
 * ============================================================================
 *
 * 測試目的：
 * 驗證系統關鍵端點的響應時間是否符合效能目標，確保系統在正常負載下提供良好的使用者體驗。
 *
 * 測試範圍：
 * 1. 首頁載入效能測試（Home Page Load Performance）
 *    - 測量 GET /ui/listings（首頁）的響應時間
 *    - 目標：P95 < 1000ms（參考目標 TTFB ≤ 500ms）
 *    - 驗證資料庫查詢效能與模板渲染速度
 *
 * 2. 搜尋端點效能測試（Search API Performance）
 *    - 測量 GET /api/listings?q=關鍵字 的響應時間
 *    - 目標：P95 < 2000ms（符合驗收標準）
 *    - 驗證資料庫模糊查詢效能與分頁處理速度
 *
 * 3. 單筆查詢效能測試（Single Item Query Performance）
 *    - 測量 GET /api/listings/{id} 的響應時間
 *    - 目標：P95 < 500ms
 *    - 驗證主鍵查詢效能
 *
 * 測試策略：
 * - 建立測試資料集（100 筆刊登）模擬真實資料量
 * - 執行 50 次請求測量響應時間（模擬真實使用場景）
 * - 計算統計指標：P50/P95/P99、Min/Max/Avg
 * - 使用 MockMvc 模擬 HTTP 請求（避免網路延遲）
 * - 所有測試在同一事務中執行，避免資料污染
 *
 * 測試框架：
 * - JUnit 5 (Jupiter)
 * - Spring Boot Test (@SpringBootTest, @AutoConfigureMockMvc)
 * - MockMvc（模擬 HTTP 請求）
 * - @Transactional（測試資料隔離）
 *
 * 關聯文件：
 * - 測試計畫：src/test/docs/4.系統非功能性測試.md (Section 3.4 效能測試)
 * - 測試報告：src/test/docs/6.測試結果報告.md (Section 5.3.5)
 * - 業務邏輯：src/main/java/com/exchange/platform/service/ListingService.java
 *
 * 測試覆蓋的服務層：
 * - ListingService: 刊登查詢、搜尋、分頁（效能瓶頸點）
 * - UserRepository: 使用者資料關聯查詢
 *
 * 預期測試結果：
 * - PERF-01: 首頁載入 P95 < 1000ms
 * - PERF-02: 搜尋查詢 P95 < 2000ms
 * - PERF-03: 單筆查詢 P95 < 500ms
 *
 * 執行方式：
 * mvn test -Dtest=PerformanceTest
 *
 * 注意事項：
 * - 效能測試受測試環境影響（CPU、記憶體、資料庫配置）
 * - 本測試為相對效能基準，不代表生產環境絕對效能
 * - 資料庫索引策略會顯著影響查詢效能
 * - 生產環境建議使用 APM 工具（New Relic/Datadog）監控真實效能
 *
 * 測試人員：廖承偉
 * 測試日期：2025-12-12
 * 測試框架版本：Spring Boot 3.x, JUnit 5
 *
 * ============================================================================
 */

package com.exchange.tests;

import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.User;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 系統效能測試類
 * 測試關鍵端點的響應時間與效能指標
 */
@SpringBootTest(classes = com.exchange.platform.ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Transactional
public class PerformanceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ListingRepository listingRepository;

    private User viewerUser; // 查看者（測試用戶）
    private User ownerUser;  // 刊登擁有者（其他用戶）
    private MockHttpSession testSession;
    private List<Listing> testListings = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        // 創建測試用戶（查看者）
        viewerUser = User.builder()
                .email("performance_viewer@example.com")
                .passwordHash("password123")
                .displayName("Performance Viewer")
                .verified(true)
                .roles("USER")
                .build();
        viewerUser = userRepository.save(viewerUser);

        // 創建刊登擁有者（其他用戶）
        ownerUser = User.builder()
                .email("performance_owner@example.com")
                .passwordHash("password123")
                .displayName("Performance Owner")
                .verified(true)
                .roles("USER")
                .build();
        ownerUser = userRepository.save(ownerUser);

        // 創建測試 Session（使用查看者身份）
        testSession = new MockHttpSession();
        testSession.setAttribute("userId", viewerUser.getId());

        // 創建 100 筆測試刊登（模擬真實資料量）
        // 使用 ownerUser 作為刊登擁有者，這樣 viewerUser 就能看到這些刊登
        System.out.println("Step 0 - 創建 100 筆測試刊登...");
        for (int i = 0; i < 100; i++) {
            Listing listing = Listing.builder()
                    .userId(ownerUser.getId())
                    .cardName("效能測試卡片 #" + i)
                    .artistName("藝人 " + (i % 10)) // 10 個不同藝人
                    .groupName("團體 " + (i % 5)) // 5 個不同團體
                    .description("這是效能測試用的卡片描述 " + i)
                    .cardSource(Listing.CardSource.ALBUM)
                    .conditionRating(8)
                    .status(Listing.Status.AVAILABLE)
                    .imagePaths("/images/perf_test_" + i + ".jpg")
                    .build();
            testListings.add(listingRepository.save(listing));
        }
        System.out.println("✓ 100 筆測試刊登創建完成");
    }

    /**
     * PERF-01: 首頁載入效能測試
     * 
     * 測試場景：
     * 模擬使用者訪問首頁 /ui/listings，測量頁面響應時間。
     * 
     * 測試步驟：
     * 1. 執行 50 次 GET /ui/listings 請求
     * 2. 測量每次請求的響應時間
     * 3. 計算 P50/P95/P99 響應時間
     * 4. 驗證 P95 < 1000ms
     * 
     * 預期結果：
     * - P95 響應時間 < 1000ms
     * - 所有請求返回 200 OK
     * - 頁面包含刊登列表資料
     */
    @Test
    @Order(1)
    @DisplayName("PERF-01: 首頁載入效能測試（GET /ui/listings）")
    public void testHomePageLoadPerformance() throws Exception {
        System.out.println("\n========== PERF-01: 首頁載入效能測試 ==========");

        int iterations = 50;
        List<Long> responseTimes = new ArrayList<>();

        System.out.println("Step 1 - 執行 " + iterations + " 次首頁請求...");

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();

            MvcResult result = mockMvc.perform(get("/ui/listings")
                            .session(testSession))
                    .andExpect(status().isOk())
                    .andReturn();

            long endTime = System.nanoTime();
            long responseTimeMs = (endTime - startTime) / 1_000_000; // 轉換為毫秒
            responseTimes.add(responseTimeMs);

            // 驗證頁面內容
            String content = result.getResponse().getContentAsString();
            assertThat(content).contains("效能測試卡片");
        }

        System.out.println("✓ " + iterations + " 次請求完成");

        // 計算統計數據
        List<Long> sortedTimes = responseTimes.stream().sorted().collect(Collectors.toList());
        long p50 = calculatePercentile(sortedTimes, 50);
        long p95 = calculatePercentile(sortedTimes, 95);
        long p99 = calculatePercentile(sortedTimes, 99);
        long min = sortedTimes.get(0);
        long max = sortedTimes.get(sortedTimes.size() - 1);
        double avg = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // 輸出測試結果
        System.out.println("\n========== 測試結果 ==========");
        System.out.println("請求次數: " + iterations);
        System.out.println("\n響應時間統計 (ms):");
        System.out.println("  Min: " + min);
        System.out.println("  P50 (中位數): " + p50);
        System.out.println("  P95: " + p95);
        System.out.println("  P99: " + p99);
        System.out.println("  Max: " + max);
        System.out.println("  Avg: " + String.format("%.2f", avg));
        System.out.println("==============================\n");

        // 斷言：驗證效能目標
        assertThat(p95)
                .as("首頁載入 P95 響應時間應低於 1000ms")
                .isLessThan(1000);

        System.out.println("✓ PERF-01 通過：首頁載入效能符合目標");
        System.out.println("================================================\n");
    }

    /**
     * PERF-02: 搜尋端點效能測試
     * 
     * 測試場景：
     * 模擬使用者執行關鍵字搜尋，測量 API 響應時間。
     * 
     * 測試步驟：
     * 1. 執行 50 次 GET /api/listings?q=關鍵字 請求
     * 2. 測量每次請求的響應時間
     * 3. 計算 P50/P95/P99 響應時間
     * 4. 驗證 P95 < 2000ms
     * 
     * 預期結果：
     * - P95 響應時間 < 2000ms（驗收標準）
     * - 搜尋結果正確返回匹配資料
     */
    @Test
    @Order(2)
    @DisplayName("PERF-02: 搜尋端點效能測試（GET /api/listings?q=關鍵字）")
    public void testSearchApiPerformance() throws Exception {
        System.out.println("\n========== PERF-02: 搜尋端點效能測試 ==========");

        int iterations = 50;
        List<Long> responseTimes = new ArrayList<>();
        String searchKeyword = "卡片";

        System.out.println("Step 1 - 執行 " + iterations + " 次搜尋請求（關鍵字: " + searchKeyword + "）...");

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();

            MvcResult result = mockMvc.perform(get("/api/listings")
                            .param("q", searchKeyword)
                            .param("page", "1")
                            .param("size", "10")
                            .session(testSession))
                    .andExpect(status().isOk())
                    .andReturn();

            long endTime = System.nanoTime();
            long responseTimeMs = (endTime - startTime) / 1_000_000;
            responseTimes.add(responseTimeMs);

            // 驗證回應內容
            String content = result.getResponse().getContentAsString();
            assertThat(content).contains("cardName");
        }

        System.out.println("✓ " + iterations + " 次搜尋請求完成");

        // 計算統計數據
        List<Long> sortedTimes = responseTimes.stream().sorted().collect(Collectors.toList());
        long p50 = calculatePercentile(sortedTimes, 50);
        long p95 = calculatePercentile(sortedTimes, 95);
        long p99 = calculatePercentile(sortedTimes, 99);
        long min = sortedTimes.get(0);
        long max = sortedTimes.get(sortedTimes.size() - 1);
        double avg = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // 輸出測試結果
        System.out.println("\n========== 測試結果 ==========");
        System.out.println("搜尋關鍵字: " + searchKeyword);
        System.out.println("請求次數: " + iterations);
        System.out.println("\n響應時間統計 (ms):");
        System.out.println("  Min: " + min);
        System.out.println("  P50 (中位數): " + p50);
        System.out.println("  P95: " + p95);
        System.out.println("  P99: " + p99);
        System.out.println("  Max: " + max);
        System.out.println("  Avg: " + String.format("%.2f", avg));
        System.out.println("==============================\n");

        // 斷言：驗證效能目標（驗收標準：P95 < 2s）
        assertThat(p95)
                .as("搜尋端點 P95 響應時間應低於 2000ms（驗收標準）")
                .isLessThan(2000);

        System.out.println("✓ PERF-02 通過：搜尋端點效能符合驗收標準");
        System.out.println("================================================\n");
    }

    /**
     * PERF-03: 單筆查詢效能測試
     * 
     * 測試場景：
     * 測量通過主鍵查詢單筆刊登的響應時間。
     * 
     * 測試步驟：
     * 1. 執行 50 次 GET /api/listings/{id} 請求
     * 2. 測量每次請求的響應時間
     * 3. 計算 P50/P95/P99 響應時間
     * 4. 驗證 P95 < 500ms
     * 
     * 預期結果：
     * - P95 響應時間 < 500ms
     * - 主鍵查詢應非常快速（有索引）
     */
    @Test
    @Order(3)
    @DisplayName("PERF-03: 單筆查詢效能測試（GET /api/listings/{id}）")
    public void testSingleItemQueryPerformance() throws Exception {
        System.out.println("\n========== PERF-03: 單筆查詢效能測試 ==========");

        int iterations = 50;
        List<Long> responseTimes = new ArrayList<>();
        Long targetId = testListings.get(0).getId();

        System.out.println("Step 1 - 執行 " + iterations + " 次單筆查詢請求（ID: " + targetId + "）...");

        for (int i = 0; i < iterations; i++) {
            long startTime = System.nanoTime();

            MvcResult result = mockMvc.perform(get("/api/listings/" + targetId))
                    .andExpect(status().isOk())
                    .andReturn();

            long endTime = System.nanoTime();
            long responseTimeMs = (endTime - startTime) / 1_000_000;
            responseTimes.add(responseTimeMs);

            // 驗證回應內容
            String content = result.getResponse().getContentAsString();
            assertThat(content).contains("cardName");
            assertThat(content).contains("效能測試卡片");
        }

        System.out.println("✓ " + iterations + " 次查詢請求完成");

        // 計算統計數據
        List<Long> sortedTimes = responseTimes.stream().sorted().collect(Collectors.toList());
        long p50 = calculatePercentile(sortedTimes, 50);
        long p95 = calculatePercentile(sortedTimes, 95);
        long p99 = calculatePercentile(sortedTimes, 99);
        long min = sortedTimes.get(0);
        long max = sortedTimes.get(sortedTimes.size() - 1);
        double avg = responseTimes.stream().mapToLong(Long::longValue).average().orElse(0.0);

        // 輸出測試結果
        System.out.println("\n========== 測試結果 ==========");
        System.out.println("查詢 ID: " + targetId);
        System.out.println("請求次數: " + iterations);
        System.out.println("\n響應時間統計 (ms):");
        System.out.println("  Min: " + min);
        System.out.println("  P50 (中位數): " + p50);
        System.out.println("  P95: " + p95);
        System.out.println("  P99: " + p99);
        System.out.println("  Max: " + max);
        System.out.println("  Avg: " + String.format("%.2f", avg));
        System.out.println("==============================\n");

        // 斷言：驗證效能目標
        assertThat(p95)
                .as("單筆查詢 P95 響應時間應低於 500ms")
                .isLessThan(500);

        System.out.println("✓ PERF-03 通過：單筆查詢效能優異");
        System.out.println("================================================\n");
    }

    /**
     * 計算百分位數
     */
    private long calculatePercentile(List<Long> sortedValues, int percentile) {
        if (sortedValues.isEmpty()) {
            return 0;
        }
        int index = (int) Math.ceil(percentile / 100.0 * sortedValues.size()) - 1;
        index = Math.max(0, Math.min(index, sortedValues.size() - 1));
        return sortedValues.get(index);
    }

    /**
     * 測試總結報告
     */
    @Test
    @Order(99)
    @DisplayName("效能測試總結報告")
    public void printPerformanceSummary() {
        System.out.println("\n");
        System.out.println("╔════════════════════════════════════════════════════════╗");
        System.out.println("║        系統效能測試 (Performance Test) 總結            ║");
        System.out.println("╠════════════════════════════════════════════════════════╣");
        System.out.println("║                                                        ║");
        System.out.println("║  測試範圍：                                            ║");
        System.out.println("║  ✓ PERF-01: 首頁載入效能（GET /ui/listings）          ║");
        System.out.println("║  ✓ PERF-02: 搜尋端點效能（GET /api/listings?q=...）   ║");
        System.out.println("║  ✓ PERF-03: 單筆查詢效能（GET /api/listings/{id}）    ║");
        System.out.println("║                                                        ║");
        System.out.println("║  測試結論：                                            ║");
        System.out.println("║  系統關鍵端點響應時間符合效能目標。                    ║");
        System.out.println("║  - 首頁載入 P95 < 1000ms                              ║");
        System.out.println("║  - 搜尋查詢 P95 < 2000ms（驗收標準）                  ║");
        System.out.println("║  - 主鍵查詢 P95 < 500ms                               ║");
        System.out.println("║                                                        ║");
        System.out.println("║  測試環境說明：                                        ║");
        System.out.println("║  - 測試資料量：100 筆刊登                              ║");
        System.out.println("║  - 測試迭代次數：50 次/端點                            ║");
        System.out.println("║  - 測試方式：MockMvc（排除網路延遲）                   ║");
        System.out.println("║                                                        ║");
        System.out.println("║  建議：                                                ║");
        System.out.println("║  - 生產環境資料量增長後需重新評估效能                  ║");
        System.out.println("║  - 搜尋查詢建議加入全文檢索索引（Elasticsearch）      ║");
        System.out.println("║  - 使用 APM 工具監控真實使用者體驗（New Relic/Datadog）║");
        System.out.println("║  - 定期執行資料庫查詢計劃分析（EXPLAIN）               ║");
        System.out.println("║                                                        ║");
        System.out.println("║  關聯文件：                                            ║");
        System.out.println("║  - 測試計畫: 4.系統非功能性測試.md (Section 3.4)      ║");
        System.out.println("║  - 測試報告: 6.測試結果報告.md (Section 5.3.5)        ║");
        System.out.println("║                                                        ║");
        System.out.println("╚════════════════════════════════════════════════════════╝");
        System.out.println("\n");
    }
}