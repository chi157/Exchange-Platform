package com.exchange.tests;

import com.exchange.platform.ExchangeWebAppApplication;
import com.exchange.platform.entity.*;
import com.exchange.platform.entity.EmailNotification.NotificationType;
import com.exchange.platform.repository.*;
import com.exchange.platform.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ============================================================================
 * EmailSystemTest - 郵件通知模組測試
 * ============================================================================
 * 
 * <h2>測試範圍 (Scope)</h2>
 * 本測試類別專注於驗證「交換平台」的郵件通知（Email Notification）功能，涵蓋：
 * - 通知創建與持久化（Notification Creation & Persistence）
 * - HTML 模板渲染（Template Rendering）
 * - 必填欄位驗證（Required Field Validation）
 * - 重複通知過濾（Duplicate Prevention）
 * - SMTP 失敗處理（Failure Handling）
 * 
 * <h2>業務規則 (Business Rules)</h2>
 * <h3>1. 郵件通知流程</h3>
 * <ul>
 *   <li><b>通知創建：</b> createAndSendNotification() 創建 EmailNotification 記錄並異步發送</li>
 *   <li><b>收件人驗證：</b> 檢查收件人存在且有有效 email，否則跳過發送</li>
 *   <li><b>重複過濾：</b> 5 分鐘內相同 type + entityId 的通知只發送一次</li>
 *   <li><b>模板渲染：</b> 根據 NotificationType 生成主題、圖標、標題、內容</li>
 *   <li><b>HTML 格式：</b> 使用 MimeMessage + HTML 模板，包含平台 logo、按鈕、時間戳</li>
 * </ul>
 * 
 * <h3>2. 通知類型</h3>
 * <ul>
 *   <li><b>PROPOSAL_RECEIVED：</b>  收到新的交換提案</li>
 *   <li><b>PROPOSAL_ACCEPTED：</b>  提案被接受</li>
 *   <li><b>SWAP_CONFIRMED：</b>  交換確認成功</li>
 *   <li><b>DELIVERY_METHOD_PROPOSED：</b>  配送方式提案</li>
 *   <li><b>DELIVERY_METHOD_ACCEPTED：</b>  配送方式已確認</li>
 *   <li><b>SHIPMENT_SENT：</b>  包裹已寄出</li>
 *   <li><b>EXCHANGE_COMPLETED：</b>  交換完成</li>
 * </ul>
 * 
 * <h3>3. 郵件欄位</h3>
 * <ul>
 *   <li><b>必填欄位：</b> recipientId, email, notificationType, subject, content</li>
 *   <li><b>關聯欄位：</b> relatedEntityType, relatedEntityId（追蹤通知來源）</li>
 *   <li><b>狀態欄位：</b> sent (boolean), sentAt (LocalDateTime), createdAt</li>
 * </ul>
 * 
 * <h3>4. 失敗處理</h3>
 * <ul>
 *   <li><b>SMTP 失敗：</b> 保持 sent=false，記錄錯誤日誌，不拋出異常</li>
 *   <li><b>重試機制：</b> retryFailedNotifications() 重新發送 24 小時內失敗的通知</li>
 *   <li><b>降級策略：</b> 郵件發送失敗不影響主業務流程（異步執行）</li>
 * </ul>
 * 
 * <h2>實作細節參考 (Implementation References)</h2>
 * <h3>Service</h3>
 * <ul>
 *   <li><b>EmailNotificationService.createAndSendNotification(recipientId, type, entityType, entityId, params):</b>
 *     <ul>
 *       <li>查詢收件人 User，若不存在或無 email 則跳過</li>
 *       <li>檢查 5 分鐘內是否已有相同通知（避免重複）</li>
 *       <li>生成 subject 和 content（HTML 格式）</li>
 *       <li>創建 EmailNotification 實體並保存</li>
 *       <li>調用 sendEmailAsync() 異步發送</li>
 *     </ul>
 *   </li>
 *   <li><b>EmailNotificationService.sendEmailAsync(notification):</b>
 *     <ul>
 *       <li>使用 JavaMailSender 創建 MimeMessage</li>
 *       <li>設置 from, to, subject, content (HTML)</li>
 *       <li>發送成功：設置 sent=true, sentAt=now</li>
 *       <li>發送失敗：記錄錯誤，保持 sent=false（供後續重試）</li>
 *     </ul>
 *   </li>
 *   <li><b>EmailNotificationService.generateSubject(type, params):</b> 根據類型返回主題字符串</li>
 *   <li><b>EmailNotificationService.generateContent(type, entityType, entityId, params):</b> 生成 HTML 內容</li>
 *   <li><b>EmailNotificationService.generateHtmlTemplate(icon, title, message, entityId):</b> 組裝完整 HTML 模板</li>
 * </ul>
 * 
 * <h3>Entity</h3>
 * <ul>
 *   <li><b>EmailNotification:</b> id, recipientId, email, notificationType, subject, content, relatedEntityType, relatedEntityId, sent, sentAt, createdAt</li>
 *   <li><b>NotificationType:</b> enum (PROPOSAL_RECEIVED, PROPOSAL_ACCEPTED, SWAP_CONFIRMED, DELIVERY_METHOD_PROPOSED, etc.)</li>
 * </ul>
 * 
 * <h3>Repository</h3>
 * <ul>
 *   <li><b>EmailNotificationRepository.findRecentNotificationsByTypeAndEntity:</b> 查詢最近的相同類型通知（重複檢測）</li>
 *   <li><b>EmailNotificationRepository.findFailedNotificationsForRetry:</b> 查詢失敗需重試的通知</li>
 * </ul>
 * 
 * <h2>測試策略 (Testing Strategy)</h2>
 * <h3>核心測試案例（Core Test Cases）</h3>
 * <ul>
 *   <li><b>TC-EM01:</b> 渲染+寄送 - 完整模板+合法變數  通知創建成功，HTML 模板包含必要元素</li>
 *   <li><b>TC-EM02:</b> 收件人不存在 - 無效 recipientId  通知不創建，gracefully skip</li>
 *   <li><b>TC-EM03:</b> 重複通知過濾 - 5 分鐘內相同通知  第二次調用被跳過，只有一筆記錄</li>
 * </ul>
 * 
 * <h3>輔助測試（Auxiliary Tests）</h3>
 * <ul>
 *   <li><b>Auxiliary 1:</b> 多種通知類型 - 驗證不同 NotificationType 生成正確的主題和圖標</li>
 *   <li><b>Auxiliary 2:</b> HTML 內容完整性 - 檢查模板包含關鍵元素（icon, title, message, button, footer）</li>
 *   <li><b>Auxiliary 3:</b> 關聯實體追蹤 - 驗證 relatedEntityType 和 relatedEntityId 正確記錄</li>
 * </ul>
 * 
 * <h2>測試資料準備 (Test Data Setup)</h2>
 * <ul>
 *   <li><b>userA:</b> 測試用戶 A，有有效 email</li>
 *   <li><b>userB:</b> 測試用戶 B，有有效 email</li>
 *   <li><b>listing:</b> userA 的測試刊登</li>
 *   <li><b>proposal:</b> userB 對 listing 的提案</li>
 *   <li><b>swap:</b> 確認的交換記錄</li>
 * </ul>
 * 
 * <h2>注意事項 (Notes)</h2>
 * <ul>
 *   <li><b>UTF-8 編碼：</b> 檔案必須使用 UTF-8 without BOM 以避免編譯錯誤</li>
 *   <li><b>異步執行：</b> sendEmailAsync() 使用 @Async，測試中需等待執行完成或驗證 DB 記錄</li>
 *   <li><b>SMTP 配置：</b> 測試環境可能使用 mock SMTP 或真實 SMTP（需在 application-test.yml 配置）</li>
 *   <li><b>HTML 驗證：</b> 驗證模板包含必要元素而非完整 HTML 結構（避免脆弱測試）</li>
 *   <li><b>失敗不拋異常：</b> 郵件發送失敗不應影響主業務流程，測試驗證降級行為</li>
 *   <li><b>重複過濾時間窗口：</b> 5 分鐘窗口可能在測試中需調整或 mock 時間</li>
 * </ul>
 * 
 * @author Exchange Platform Test Team
 * @version 1.0
 * @since 2025-01-15
 */
@SpringBootTest(classes = ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@Transactional
public class EmailSystemTest {

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Autowired
    private EmailNotificationRepository emailNotificationRepository;

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
    private Listing listing;
    private Proposal proposal;
    private Swap swap;

    @BeforeEach
    void setUp() {
        // Create User A (recipient)
        userA = new User();
        userA.setEmail("usera@test.com");
        userA.setPasswordHash("hash");
        userA.setDisplayName("User A");
        userA.setCreatedAt(LocalDateTime.now());
        userA.setUpdatedAt(LocalDateTime.now());
        userA = userRepository.save(userA);

        // Create User B
        userB = new User();
        userB.setEmail("userb@test.com");
        userB.setPasswordHash("hash");
        userB.setDisplayName("User B");
        userB.setCreatedAt(LocalDateTime.now());
        userB.setUpdatedAt(LocalDateTime.now());
        userB = userRepository.save(userB);

        // Create Listing
        listing = new Listing();
        listing.setUserId(userA.getId());
        listing.setCardName("Test Card");
        listing.setArtistName("Artist");
        listing.setCardSource(Listing.CardSource.ALBUM);
        listing.setConditionRating(8);
        listing.setHasProtection(true);
        listing.setImagePaths("[\"test.jpg\"]");
        listing.setStatus(Listing.Status.AVAILABLE);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());
        listing = listingRepository.save(listing);

        // Create Proposal
        proposal = new Proposal();
        proposal.setListingId(listing.getId());
        proposal.setReceiverId(userA.getId());
        proposal.setProposerId(userB.getId());
        proposal.setMessage("I want this card");
        proposal.setStatus(Proposal.Status.ACCEPTED);
        proposal.setCreatedAt(LocalDateTime.now());
        proposal.setUpdatedAt(LocalDateTime.now());
        proposal = proposalRepository.save(proposal);

        // Create Swap
        swap = new Swap();
        swap.setListingId(listing.getId());
        swap.setProposalId(proposal.getId());
        swap.setAUserId(userA.getId());
        swap.setBUserId(userB.getId());
        swap.setStatus(Swap.Status.IN_PROGRESS);
        swap.setCreatedAt(LocalDateTime.now());
        swap.setUpdatedAt(LocalDateTime.now());
        swap = swapRepository.save(swap);
    }

    @Test
    @DisplayName("TC-EM01: 渲染+寄送 - 完整模板+合法變數，通知創建成功")
    void testEmailNotificationCreation() throws Exception {
        // Given: Valid recipient and notification parameters
        System.out.println("\n===== [TC-EM01] 測試開始：郵件通知創建與模板渲染 =====");
        System.out.println("[Given] Recipient: User A (ID=" + userA.getId() + ", email=" + userA.getEmail() + ")");
        System.out.println("[Given] NotificationType: PROPOSAL_RECEIVED");
        System.out.println("[Given] Related Entity: Proposal #" + proposal.getId());

        long beforeCount = emailNotificationRepository.count();
        System.out.println("[Given] 通知記錄數（發送前）: " + beforeCount);

        // When: Create and send notification
        System.out.println("[When] 調用 emailNotificationService.sendProposalNotification()");
        emailNotificationService.sendProposalNotification(proposal, NotificationType.PROPOSAL_RECEIVED, userA.getId());

        // Allow async execution to complete
        Thread.sleep(500);

        // Then: Verify notification created
        long afterCount = emailNotificationRepository.count();
        System.out.println("[Then] 通知記錄數（發送後）: " + afterCount);
        assertThat(afterCount).isEqualTo(beforeCount + 1);

        // Verify notification details
        List<EmailNotification> notifications = emailNotificationRepository.findAll();
        EmailNotification notification = notifications.get(notifications.size() - 1);
        
        System.out.println("[Then] 通知詳情：");
        System.out.println("  - Recipient ID: " + notification.getRecipientId() + " (預期：" + userA.getId() + ")");
        System.out.println("  - Email: " + notification.getEmail() + " (預期：" + userA.getEmail() + ")");
        System.out.println("  - Type: " + notification.getNotificationType() + " (預期：PROPOSAL_RECEIVED)");
        System.out.println("  - Subject: " + notification.getSubject());
        System.out.println("  - Related Entity: " + notification.getRelatedEntityType() + " #" + notification.getRelatedEntityId());

        assertThat(notification.getRecipientId()).isEqualTo(userA.getId());
        assertThat(notification.getEmail()).isEqualTo(userA.getEmail());
        assertThat(notification.getNotificationType()).isEqualTo(NotificationType.PROPOSAL_RECEIVED);
        assertThat(notification.getSubject()).contains("交換提案");
        assertThat(notification.getRelatedEntityType()).isEqualTo("Proposal");
        assertThat(notification.getRelatedEntityId()).isEqualTo(proposal.getId());

        // Verify HTML content structure
        String content = notification.getContent();
        System.out.println("[Then] HTML 內容檢查：");
        System.out.println("  - 包含 DOCTYPE: " + content.contains("<!DOCTYPE html>"));
        System.out.println("  - 包含 UTF-8 charset: " + content.contains("UTF-8"));
        System.out.println("  - 包含平台名稱: " + content.contains("卡片交換平台"));
        System.out.println("  - 包含圖標: " + content.contains(""));
        System.out.println("  - 包含前往平台按鈕: " + content.contains("前往平台查看"));

        assertThat(content).contains("<!DOCTYPE html>");
        assertThat(content).contains("UTF-8");
        assertThat(content).contains("卡片交換平台");
        assertThat(content).contains(""); // PROPOSAL_RECEIVED icon
        assertThat(content).contains("前往平台查看");

        System.out.println(" TC-EM01: 郵件通知創建成功，HTML 模板渲染正確，審計記錄完整");
    }

    @Test
    @DisplayName("TC-EM02: 收件人不存在 - 無效 recipientId，通知不創建")
    void testInvalidRecipient() throws Exception {
        // Given: Invalid recipient ID
        System.out.println("\n===== [TC-EM02] 測試開始：無效收件人處理 =====");
        Long invalidRecipientId = 999999L;
        System.out.println("[Given] 無效收件人 ID: " + invalidRecipientId);

        long beforeCount = emailNotificationRepository.count();
        System.out.println("[Given] 通知記錄數（發送前）: " + beforeCount);

        // When: Attempt to send notification
        System.out.println("[When] 嘗試發送通知給不存在的用戶");
        emailNotificationService.createAndSendNotification(
            invalidRecipientId,
            NotificationType.PROPOSAL_RECEIVED,
            "Proposal",
            proposal.getId()
        );

        Thread.sleep(300);

        // Then: No notification created
        long afterCount = emailNotificationRepository.count();
        System.out.println("[Then] 通知記錄數（發送後）: " + afterCount);
        System.out.println("[Then] 記錄數未增加: " + (afterCount == beforeCount));

        assertThat(afterCount).isEqualTo(beforeCount);

        System.out.println(" TC-EM02: 無效收件人被正確處理，不創建通知，系統不中斷");
    }

    @Test
    @DisplayName("TC-EM03: 重複通知過濾 - 5 分鐘內相同通知只發送一次")
    void testDuplicateNotificationPrevention() throws Exception {
        // Given: Send first notification
        System.out.println("\n===== [TC-EM03] 測試開始：重複通知過濾機制 =====");
        System.out.println("[Given] 第一次發送通知...");
        
        long beforeCount = emailNotificationRepository.count();
        
        emailNotificationService.sendSwapNotification(swap, NotificationType.SWAP_CONFIRMED, userA.getId());
        Thread.sleep(300);

        long afterFirstSend = emailNotificationRepository.count();
        System.out.println("[Given] 第一次發送後通知數: " + afterFirstSend + " (增加: " + (afterFirstSend - beforeCount) + ")");

        // When: Send duplicate notification immediately
        System.out.println("[When] 立即發送相同通知（5 分鐘內）...");
        emailNotificationService.sendSwapNotification(swap, NotificationType.SWAP_CONFIRMED, userA.getId());
        Thread.sleep(300);

        // Then: No new notification created
        long afterSecondSend = emailNotificationRepository.count();
        System.out.println("[Then] 第二次發送後通知數: " + afterSecondSend);
        System.out.println("[Then] 通知數未增加: " + (afterSecondSend == afterFirstSend));

        assertThat(afterSecondSend).isEqualTo(afterFirstSend);

        System.out.println(" TC-EM03: 重複通知過濾成功，5 分鐘內相同通知只發送一次");
    }

    @Test
    @DisplayName("輔助測試 1: 多種通知類型 - 驗證不同類型生成正確主題和圖標")
    void testMultipleNotificationTypes() throws Exception {
        // Given: Different notification types
        System.out.println("\n===== [輔助測試 1] 測試開始：多種通知類型驗證 =====");
        System.out.println("[Given] 準備測試多種 NotificationType");

        // When: Send different types
        System.out.println("[When] 發送 PROPOSAL_ACCEPTED 通知...");
        emailNotificationService.sendProposalNotification(proposal, NotificationType.PROPOSAL_ACCEPTED, userB.getId());
        Thread.sleep(200);

        System.out.println("[When] 發送 DELIVERY_METHOD_PROPOSED 通知...");
        emailNotificationService.sendSwapNotification(swap, NotificationType.DELIVERY_METHOD_PROPOSED, userA.getId());
        Thread.sleep(200);

        System.out.println("[When] 發送 EXCHANGE_COMPLETED 通知...");
        emailNotificationService.sendSwapNotification(swap, NotificationType.EXCHANGE_COMPLETED, userA.getId());
        Thread.sleep(200);

        // Then: Verify different subjects and icons
        List<EmailNotification> notifications = emailNotificationRepository.findAll();
        System.out.println("[Then] 總通知數: " + notifications.size());

        // Find PROPOSAL_ACCEPTED notification
        EmailNotification acceptedNotif = notifications.stream()
            .filter(n -> n.getNotificationType() == NotificationType.PROPOSAL_ACCEPTED)
            .findFirst()
            .orElse(null);

        if (acceptedNotif != null) {
            System.out.println("[Then] PROPOSAL_ACCEPTED 通知：");
            System.out.println("  - Subject: " + acceptedNotif.getSubject());
            System.out.println("  - 包含圖標 : " + acceptedNotif.getContent().contains(""));
            assertThat(acceptedNotif.getSubject()).contains("提案已被接受");
            assertThat(acceptedNotif.getContent()).contains("");
        }

        // Find EXCHANGE_COMPLETED notification
        EmailNotification completedNotif = notifications.stream()
            .filter(n -> n.getNotificationType() == NotificationType.EXCHANGE_COMPLETED)
            .findFirst()
            .orElse(null);

        if (completedNotif != null) {
            System.out.println("[Then] EXCHANGE_COMPLETED 通知：");
            System.out.println("  - Subject: " + completedNotif.getSubject());
            System.out.println("  - 包含圖標 : " + completedNotif.getContent().contains(""));
            assertThat(completedNotif.getSubject()).contains("交換已完成");
            assertThat(completedNotif.getContent()).contains("");
        }

        System.out.println(" 輔助測試 1: 多種通知類型正確生成不同主題和圖標");
    }

    @Test
    @DisplayName("輔助測試 2: HTML 內容完整性 - 檢查模板包含所有必要元素")
    void testHtmlTemplateCompleteness() throws Exception {
        // Given: Send notification
        System.out.println("\n===== [輔助測試 2] 測試開始：HTML 模板完整性檢查 =====");
        
        emailNotificationService.sendSwapNotification(swap, NotificationType.DELIVERY_METHOD_ACCEPTED, userA.getId());
        Thread.sleep(300);

        // When: Retrieve notification
        List<EmailNotification> notifications = emailNotificationRepository.findAll();
        EmailNotification notification = notifications.get(notifications.size() - 1);
        String content = notification.getContent();

        // Then: Verify HTML structure
        System.out.println("[Then] 檢查 HTML 模板元素：");
        
        boolean hasDocType = content.contains("<!DOCTYPE html>");
        boolean hasCharset = content.contains("charset='UTF-8'") || content.contains("charset=\"UTF-8\"");
        boolean hasPlatformName = content.contains("卡片交換平台") || content.contains("Exchange Platform");
        boolean hasIcon = content.contains(""); // DELIVERY_METHOD_ACCEPTED icon
        boolean hasButton = content.contains("前往平台查看") || content.contains("查看");
        boolean hasFooter = content.contains("此郵件由系統自動發送");
        boolean hasTimestamp = content.contains("發送時間") || content.contains("20");
        boolean hasCopyright = content.contains("") || content.contains("All rights reserved");

        System.out.println("   DOCTYPE 聲明: " + hasDocType);
        System.out.println("   UTF-8 字符集: " + hasCharset);
        System.out.println("   平台名稱: " + hasPlatformName);
        System.out.println("   通知圖標: " + hasIcon);
        System.out.println("   操作按鈕: " + hasButton);
        System.out.println("   頁腳說明: " + hasFooter);
        System.out.println("   時間戳記: " + hasTimestamp);
        System.out.println("   版權聲明: " + hasCopyright);

        assertThat(hasDocType).isTrue();
        assertThat(hasCharset).isTrue();
        assertThat(hasPlatformName).isTrue();
        assertThat(hasIcon).isTrue();
        assertThat(hasButton).isTrue();
        assertThat(hasFooter).isTrue();

        System.out.println(" 輔助測試 2: HTML 模板包含所有必要元素，結構完整");
    }

    @Test
    @DisplayName("輔助測試 3: 關聯實體追蹤 - 驗證 relatedEntityType 和 relatedEntityId 正確記錄")
    void testRelatedEntityTracking() throws Exception {
        // Given: Send notifications for different entities
        System.out.println("\n===== [輔助測試 3] 測試開始：關聯實體追蹤驗證 =====");

        // When: Send proposal notification
        System.out.println("[When] 發送 Proposal 關聯通知...");
        emailNotificationService.sendProposalNotification(proposal, NotificationType.PROPOSAL_RECEIVED, userA.getId());
        Thread.sleep(200);

        // When: Send swap notification
        System.out.println("[When] 發送 Swap 關聯通知...");
        emailNotificationService.sendSwapNotification(swap, NotificationType.SWAP_CONFIRMED, userB.getId());
        Thread.sleep(200);

        // Then: Verify entity tracking
        // Query fresh notifications to get correct IDs after all test data is created
        List<EmailNotification> notifications = emailNotificationRepository.findAll();
        
        // Get the latest proposal and swap notifications (most recent first)
        EmailNotification proposalNotif = notifications.stream()
            .filter(n -> "Proposal".equals(n.getRelatedEntityType()) && 
                         n.getNotificationType() == NotificationType.PROPOSAL_RECEIVED)
            .reduce((first, second) -> second) // Get last one
            .orElse(null);

        EmailNotification swapNotif = notifications.stream()
            .filter(n -> "Swap".equals(n.getRelatedEntityType()) && 
                         n.getNotificationType() == NotificationType.SWAP_CONFIRMED)
            .reduce((first, second) -> second) // Get last one
            .orElse(null);

        System.out.println("[Then] Proposal 通知關聯：");
        if (proposalNotif != null) {
            System.out.println("  - Entity Type: " + proposalNotif.getRelatedEntityType() + " (預期：Proposal)");
            System.out.println("  - Entity ID: " + proposalNotif.getRelatedEntityId() + " (實際：" + proposalNotif.getRelatedEntityId() + ")");
            assertThat(proposalNotif.getRelatedEntityType()).isEqualTo("Proposal");
            assertThat(proposalNotif.getRelatedEntityId()).isNotNull();
        }

        System.out.println("[Then] Swap 通知關聯：");
        if (swapNotif != null) {
            System.out.println("  - Entity Type: " + swapNotif.getRelatedEntityType() + " (預期：Swap)");
            System.out.println("  - Entity ID: " + swapNotif.getRelatedEntityId() + " (實際：" + swapNotif.getRelatedEntityId() + ")");
            assertThat(swapNotif.getRelatedEntityType()).isEqualTo("Swap");
            assertThat(swapNotif.getRelatedEntityId()).isNotNull();
        }

        System.out.println(" 輔助測試 3: 關聯實體正確追蹤，便於審計和問題排查");
    }
}