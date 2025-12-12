/*
 * 模組 F：配送設定與物流追蹤系統測試 (ShipmentSystemTest.java)
 * 
 * =====================================================
 * 測試範圍與目的
 * =====================================================
 * 本測試類別驗證配送設定與物流追蹤的核心功能，包括：
 * 1. 配送方式設定（面交 FACE_TO_FACE / 交貨便 SHIPNOW）
 * 2. 面交資訊（地點、時間、備註）
 * 3. 交貨便資訊（門市、追蹤號碼、追蹤 URL）
 * 4. 物流事件追蹤（事件序列、狀態更新）
 * 5. 權限控制（只有發送者可以更新配送資訊）
 * 
 * =====================================================
 * 業務規則說明
 * =====================================================
 * 
 * 【配送方式類型】
 * - FACE_TO_FACE（面交）：
 *   - 需要設定面交地點、時間、備註
 *   - 不需要追蹤號碼和 URL
 *   - 雙方需要確認面交時間和地點
 * 
 * - SHIPNOW（交貨便/7-11 賣貨便）：
 *   - 需要設定偏好收件門市（preferredStore711）
 *   - 需要追蹤號碼（trackingNumber）和追蹤 URL
 *   - 系統可透過追蹤號碼查詢物流狀態
 * 
 * 【配送設定流程】
 * 1. 交換提案被接受後，創建 Swap 記錄
 * 2. 雙方各自設定自己的配送方式（Shipment）
 * 3. 發送者（sender）可以建立或更新配送資訊
 * 4. 每個 Swap 的每個 sender 只能有一筆 Shipment（unique constraint）
 * 5. 配送方式一旦選擇，相關欄位需符合限制：
 *    - FACE_TO_FACE：trackingNumber, trackingUrl, preferredStore711 必須為 null
 *    - SHIPNOW：preferredStore711 可選，trackingNumber 和 trackingUrl 可後續補充
 * 
 * 【物流事件追蹤】
 * - 每個 Shipment 可以有多個 ShipmentEvent
 * - 事件包含：status（狀態）、note（備註）、at（事件時間）
 * - 常見狀態：CREATED, PICKED_UP, IN_TRANSIT, DELIVERED, FAILED
 * - lastStatus 欄位記錄最新狀態
 * - 只有發送者可以新增事件
 * 
 * 【權限控制】
 * - 未登入用戶：401 Unauthorized
 * - 非 Swap 參與者：403 Forbidden
 * - 只有發送者可以更新自己的 Shipment
 * - 只有發送者可以新增 ShipmentEvent
 * 
 * =====================================================
 * 實作細節參考
 * =====================================================
 * 
 * 【Controller】
 * - ShipmentController.java
 *   - POST /api/swaps/{id}/shipments/my：建立或更新自己的配送資訊
 *   - POST /api/shipments/{id}/events：新增物流事件（僅限發送者）
 *   - 異常處理：UnauthorizedException (401), ForbiddenException (403), 
 *               NotFoundException (404), BadRequestException (400)
 * 
 * 【Service】
 * - ShipmentService.java
 *   - upsertMyShipment(swapId, request, session)：建立或更新配送資訊
 *     - 驗證用戶是否為 Swap 參與者
 *     - 解析 deliveryMethod（shipnow 或 face_to_face，不分大小寫）
 *     - 根據配送方式清除不相關欄位
 *     - 自動設定 receiverIdLegacy（相容性欄位）
 *     - 如果有追蹤號碼且之前未寄出，發送郵件通知
 *   - addEvent(shipmentId, request, session)：新增物流事件
 *     - 驗證用戶是否為發送者
 *     - 建立 ShipmentEvent 記錄
 *     - 更新 Shipment 的 lastStatus
 * 
 * 【Entity】
 * - Shipment.java
 *   - 欄位：id, swapId, senderId, receiverIdLegacy, deliveryMethod, 
 *          preferredStore711, trackingNumber, trackingUrl, lastStatus, 
 *          shippedAt, createdAt, updatedAt
 *   - DeliveryMethod 枚舉：SHIPNOW, FACE_TO_FACE
 *   - 唯一約束：(swap_id, sender_id)
 * 
 * - ShipmentEvent.java
 *   - 欄位：id, shipmentId, status, note, at, atLegacy, createdAt
 *   - at 和 atLegacy 需同步（相容性欄位）
 * 
 * - Swap.java
 *   - 欄位：id, aUserId, bUserId, deliveryMethod, meetupLocation, 
 *          meetupTime, meetupNotes, aMeetupConfirmed, bMeetupConfirmed
 *   - Status 枚舉：PENDING, IN_PROGRESS, COMPLETED, CANCELED
 * 
 * 【DTO】
 * - UpsertShipmentRequest.java
 *   - deliveryMethod：必填，只接受 "shipnow" 或 "face_to_face"（不分大小寫）
 *   - preferredStore711：可選
 *   - trackingNumber：可選
 *   - trackingUrl：可選
 * 
 * - CreateShipmentEventRequest.java
 *   - status：必填
 *   - note：可選
 *   - at：必填（事件發生時間）
 * 
 * =====================================================
 * 測試策略
 * =====================================================
 * 
 * 【測試環境配置】
 * - @SpringBootTest：完整 Spring Boot 應用程式上下文
 * - @AutoConfigureMockMvc：自動配置 MockMvc
 * - @Transactional：每個測試結束後自動回滾，確保測試隔離
 * - @BeforeEach：在每個測試前創建測試資料
 * 
 * 【測試資料設計】
 * - 用戶 A（userA）：Swap 的 A 方（listing owner）
 * - 用戶 B（userB）：Swap 的 B 方（proposer）
 * - 刊登 A：userA 的刊登
 * - 提案：userB 向 userA 的刊登提出提案，狀態為 ACCEPTED
 * - Swap：提案接受後創建的交換記錄，狀態為 IN_PROGRESS
 * - MockHttpSession：模擬登入狀態
 * 
 * 【測試案例對應】
 * - TC-SH01：設定面交（FACE_TO_FACE） 200 OK，記錄更新，相關欄位正確
 * - TC-SH02：設定交貨便（SHIPNOW） 200 OK，記錄更新，追蹤資訊正確
 * - TC-SH03：非法配送方式代碼  400 Bad Request，記錄未更新
 * - TC-TR01：新增物流事件（事件序列） 201 Created，狀態更新
 * - TC-TR02：非發送者新增事件  403 Forbidden
 * - TC-TR03：未登入用戶嘗試設定配送  401 Unauthorized
 * 
 * 【驗證方式】
 * - MockMvc HTTP 狀態碼驗證
 * - JSON 回應內容驗證（$.deliveryMethod, $.trackingNumber）
 * - 資料庫狀態驗證（Shipment、ShipmentEvent 是否正確儲存）
 * - 欄位清除驗證（FACE_TO_FACE 時追蹤資訊應為 null）
 * - Console 輸出標記（ 測試通過）
 * 
 * =====================================================
 * 注意事項
 * =====================================================
 * 1. deliveryMethod 接受 "shipnow" 或 "face_to_face"（不分大小寫）
 * 2. 回應中的 deliveryMethod 為枚舉：SHIPNOW 或 FACE_TO_FACE
 * 3. FACE_TO_FACE 時，trackingNumber、trackingUrl、preferredStore711 會被清除為 null
 * 4. 每個 Swap 的每個 sender 只能有一筆 Shipment（upsert 邏輯）
 * 5. ShipmentEvent 的 at 和 atLegacy 欄位需同步（相容性需求）
 * 6. receiverIdLegacy 欄位會自動設定為 Swap 中的另一方用戶 ID
 * 7. 如果 trackingNumber 從無到有，會發送郵件通知給接收者
 * 
 * =====================================================
 * 測試執行方式
 * =====================================================
 * 命令：mvn test -Dtest=ShipmentSystemTest
 * 或：在 IDE 中右鍵執行整個測試類別
 * 
 * @author 陳欣妤（測試工程師）
 * @date 2025/12/12
 * @version 1.0
 */
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = ExchangeWebAppApplication.class)
@AutoConfigureMockMvc
@Transactional
public class ShipmentSystemTest {

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

    @Autowired
    private ShipmentRepository shipmentRepository;

    @Autowired
    private ShipmentEventRepository shipmentEventRepository;

    private User userA;         // Swap A 方（listing owner）
    private User userB;         // Swap B 方（proposer）
    private Listing listing;
    private Proposal proposal;
    private Swap swap;

    private MockHttpSession sessionA;
    private MockHttpSession sessionB;

    @BeforeEach
    public void setUp() {
        // 清理測試資料
        shipmentEventRepository.deleteAll();
        shipmentRepository.deleteAll();
        swapRepository.deleteAll();
        proposalRepository.deleteAll();
        listingRepository.deleteAll();
        // userRepository.deleteAll(); // 避免外鍵約束衝突

        // 創建測試用戶 A（listing owner）
        userA = new User();
        userA.setEmail("userA@test.com");
        userA.setPasswordHash("$2a$10$TestHashA");
        userA.setDisplayName("User A");
        userA.setVerified(true);
        userA = userRepository.save(userA);

        // 創建測試用戶 B（proposer）
        userB = new User();
        userB.setEmail("userB@test.com");
        userB.setPasswordHash("$2a$10$TestHashB");
        userB.setDisplayName("User B");
        userB.setVerified(true);
        userB = userRepository.save(userB);

        // 創建刊登
        listing = new Listing();
        listing.setUserId(userA.getId());
        listing.setCardName("Test Card for Shipment");
        listing.setArtistName("Artist");
        listing.setGroupName("Group");
        listing.setCardSource(Listing.CardSource.ALBUM);
        listing.setConditionRating(9);
        listing.setHasProtection(true);
        listing.setStatus(Listing.Status.AVAILABLE);
        listing.setImagePaths("image.jpg");
        listing = listingRepository.save(listing);

        // 創建提案（已接受）
        proposal = new Proposal();
        proposal.setProposerId(userB.getId());
        proposal.setReceiverId(userA.getId());
        proposal.setListingId(listing.getId());
        proposal.setMessage("Test proposal for shipment testing");
        proposal.setStatus(Proposal.Status.ACCEPTED);
        proposal = proposalRepository.save(proposal);

        // 創建 Swap（交換進行中）
        swap = new Swap();
        swap.setListingId(listing.getId());
        swap.setProposalId(proposal.getId());
        swap.setAUserId(userA.getId());
        swap.setBUserId(userB.getId());
        swap.setStatus(Swap.Status.IN_PROGRESS);
        swap = swapRepository.save(swap);

        // 創建 Mock Session
        sessionA = new MockHttpSession();
        sessionA.setAttribute("userId", userA.getId());
        sessionA.setAttribute("email", userA.getEmail());

        sessionB = new MockHttpSession();
        sessionB.setAttribute("userId", userB.getId());
        sessionB.setAttribute("email", userB.getEmail());
    }

    // TC-SH01：設定面交（FACE_TO_FACE）
    @Test
    @DisplayName("TC-SH01：設定面交，記錄正確更新，追蹤資訊清除")
    public void testUpsertShipment_FaceToFace_Success() throws Exception {
        // Given：面交配送請求
        String requestBody = """
                {
                    "deliveryMethod": "face_to_face"
                }
                """;

        // When：userA 設定面交配送方式
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryMethod", is("FACE_TO_FACE")))
                .andExpect(jsonPath("$.trackingNumber").doesNotExist())
                .andExpect(jsonPath("$.trackingUrl").doesNotExist())
                .andExpect(jsonPath("$.preferredStore711").doesNotExist())
                .andExpect(jsonPath("$.swapId", is(swap.getId().intValue())))
                .andExpect(jsonPath("$.senderId", is(userA.getId().intValue())));

        // Then：驗證資料庫記錄
        Shipment savedShipment = shipmentRepository.findBySwapIdAndSenderId(swap.getId(), userA.getId())
                .orElseThrow(() -> new AssertionError("Shipment 未儲存"));

        assert savedShipment.getDeliveryMethod() == Shipment.DeliveryMethod.FACE_TO_FACE;
        assert savedShipment.getTrackingNumber() == null : "面交時 trackingNumber 應為 null";
        assert savedShipment.getTrackingUrl() == null : "面交時 trackingUrl 應為 null";
        assert savedShipment.getPreferredStore711() == null : "面交時 preferredStore711 應為 null";
        assert savedShipment.getSenderId().equals(userA.getId());
        assert savedShipment.getSwapId().equals(swap.getId());

        System.out.println(" TC-SH01 通過：面交設定成功，deliveryMethod=FACE_TO_FACE，追蹤資訊已清除");
    }

    // TC-SH02：設定交貨便（SHIPNOW）
    @Test
    @DisplayName("TC-SH02：設定交貨便，記錄正確更新，追蹤資訊填寫")
    public void testUpsertShipment_ShipNow_Success() throws Exception {
        // Given：交貨便配送請求
        String requestBody = """
                {
                    "deliveryMethod": "shipnow",
                    "preferredStore711": "台北市中正區羅斯福路一段7-11門市",
                    "trackingNumber": "TW123456789",
                    "trackingUrl": "https://track.711.com/TW123456789"
                }
                """;

        // When：userB 設定交貨便配送方式
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryMethod", is("SHIPNOW")))
                .andExpect(jsonPath("$.trackingNumber", is("TW123456789")))
                .andExpect(jsonPath("$.trackingUrl", is("https://track.711.com/TW123456789")))
                .andExpect(jsonPath("$.preferredStore711", is("台北市中正區羅斯福路一段7-11門市")))
                .andExpect(jsonPath("$.swapId", is(swap.getId().intValue())))
                .andExpect(jsonPath("$.senderId", is(userB.getId().intValue())));

        // Then：驗證資料庫記錄
        Shipment savedShipment = shipmentRepository.findBySwapIdAndSenderId(swap.getId(), userB.getId())
                .orElseThrow(() -> new AssertionError("Shipment 未儲存"));

        assert savedShipment.getDeliveryMethod() == Shipment.DeliveryMethod.SHIPNOW;
        assert "TW123456789".equals(savedShipment.getTrackingNumber());
        assert "https://track.711.com/TW123456789".equals(savedShipment.getTrackingUrl());
        assert "台北市中正區羅斯福路一段7-11門市".equals(savedShipment.getPreferredStore711());
        assert savedShipment.getSenderId().equals(userB.getId());

        System.out.println(" TC-SH02 通過：交貨便設定成功，deliveryMethod=SHIPNOW，追蹤資訊正確");
    }

    // TC-SH03：非法配送方式代碼
    @Test
    @DisplayName("TC-SH03：非法配送方式代碼，回傳 400 Bad Request，記錄未更新")
    public void testUpsertShipment_InvalidMethod_BadRequest() throws Exception {
        // Given：非法配送方式請求
        String requestBody = """
                {
                    "deliveryMethod": "INVALID_METHOD"
                }
                """;

        // When & Then：期望 400 Bad Request（驗證錯誤）
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        // 驗證資料庫未新增記錄
        long count = shipmentRepository.count();
        assert count == 0 : "非法配送方式不應儲存記錄";

        System.out.println(" TC-SH03 通過：非法配送方式被正確拒絕，回傳 400 Bad Request");
    }

    // TC-SH03 額外測試：空值配送方式
    @Test
    @DisplayName("TC-SH03 額外：空值配送方式，回傳 400 Bad Request")
    public void testUpsertShipment_EmptyMethod_BadRequest() throws Exception {
        // Given：空值配送方式請求
        String requestBody = """
                {
                    "deliveryMethod": ""
                }
                """;

        // When & Then：期望 400 Bad Request
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

        System.out.println(" TC-SH03 額外測試通過：空值配送方式被正確拒絕");
    }

    // TC-TR01：新增物流事件（事件序列）
    @Test
    @DisplayName("TC-TR01：新增物流事件，狀態正確更新，事件序列記錄")
    public void testAddShipmentEvent_Success() throws Exception {
        // Given：先建立 Shipment
        Shipment shipment = Shipment.builder()
                .swapId(swap.getId())
                .senderId(userA.getId())
                .receiverIdLegacy(userB.getId())
                .deliveryMethod(Shipment.DeliveryMethod.SHIPNOW)
                .trackingNumber("TW123456789")
                .lastStatus("CREATED")
                .build();
        shipment = shipmentRepository.save(shipment);

        // Given：新增物流事件請求
        LocalDateTime eventTime = LocalDateTime.now();
        String requestBody = String.format("""
                {
                    "status": "PICKED_UP",
                    "note": "已從門市取件",
                    "at": "%s"
                }
                """, eventTime.toString());

        // When：userA（發送者）新增物流事件
        mockMvc.perform(post("/api/shipments/" + shipment.getId() + "/events")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        // Then：驗證 ShipmentEvent 已儲存
        long eventCount = shipmentEventRepository.count();
        assert eventCount == 1 : "應該有 1 筆 ShipmentEvent 記錄";

        ShipmentEvent savedEvent = shipmentEventRepository.findAll().get(0);
        assert "PICKED_UP".equals(savedEvent.getStatus());
        assert "已從門市取件".equals(savedEvent.getNote());
        assert savedEvent.getShipmentId().equals(shipment.getId());

        // 驗證 Shipment 的 lastStatus 已更新
        Shipment updatedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assert "PICKED_UP".equals(updatedShipment.getLastStatus()) : "lastStatus 應更新為 PICKED_UP";

        System.out.println(" TC-TR01 通過：物流事件新增成功，status=PICKED_UP，lastStatus 已更新");
    }

    // TC-TR01 額外測試：多個事件序列
    @Test
    @DisplayName("TC-TR01 額外：新增多個物流事件，序列正確記錄")
    public void testAddShipmentEvent_MultipleEvents_Success() throws Exception {
        // Given：先建立 Shipment
        Shipment shipment = Shipment.builder()
                .swapId(swap.getId())
                .senderId(userB.getId())
                .receiverIdLegacy(userA.getId())
                .deliveryMethod(Shipment.DeliveryMethod.SHIPNOW)
                .trackingNumber("TW987654321")
                .lastStatus("CREATED")
                .build();
        shipment = shipmentRepository.save(shipment);

        // When：新增多個事件
        String[] statuses = {"PICKED_UP", "IN_TRANSIT", "DELIVERED"};
        LocalDateTime eventTime = LocalDateTime.now();

        for (String status : statuses) {
            String requestBody = String.format("""
                    {
                        "status": "%s",
                        "note": "事件：%s",
                        "at": "%s"
                    }
                    """, status, status, eventTime.toString());

            mockMvc.perform(post("/api/shipments/" + shipment.getId() + "/events")
                            .session(sessionB)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated());

            eventTime = eventTime.plusHours(1);
        }

        // Then：驗證所有事件已儲存
        long eventCount = shipmentEventRepository.count();
        assert eventCount == 3 : "應該有 3 筆 ShipmentEvent 記錄";

        // 驗證 lastStatus 為最後一個事件的狀態
        Shipment updatedShipment = shipmentRepository.findById(shipment.getId()).orElseThrow();
        assert "DELIVERED".equals(updatedShipment.getLastStatus()) : "lastStatus 應為最後一個事件 DELIVERED";

        System.out.println(" TC-TR01 額外測試通過：多個物流事件序列正確記錄，lastStatus=DELIVERED");
    }

    // TC-TR02：非發送者新增事件
    @Test
    @DisplayName("TC-TR02：非發送者新增物流事件，回傳 403 Forbidden")
    public void testAddShipmentEvent_NonSender_Forbidden() throws Exception {
        // Given：userA 建立的 Shipment
        Shipment shipment = Shipment.builder()
                .swapId(swap.getId())
                .senderId(userA.getId())
                .receiverIdLegacy(userB.getId())
                .deliveryMethod(Shipment.DeliveryMethod.SHIPNOW)
                .trackingNumber("TW123456789")
                .lastStatus("CREATED")
                .build();
        shipment = shipmentRepository.save(shipment);

        // Given：新增物流事件請求
        LocalDateTime eventTime = LocalDateTime.now();
        String requestBody = String.format("""
                {
                    "status": "PICKED_UP",
                    "note": "嘗試新增事件",
                    "at": "%s"
                }
                """, eventTime.toString());

        // When & Then：userB（非發送者）嘗試新增事件  403 Forbidden
        mockMvc.perform(post("/api/shipments/" + shipment.getId() + "/events")
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        // 驗證事件未儲存
        long eventCount = shipmentEventRepository.count();
        assert eventCount == 0 : "非發送者不應能新增事件";

        System.out.println(" TC-TR02 通過：非發送者無法新增物流事件，回傳 403 Forbidden");
    }

    // TC-TR03：未登入用戶嘗試設定配送
    @Test
    @DisplayName("TC-TR03：未登入用戶嘗試設定配送，回傳 401 Unauthorized")
    public void testUpsertShipment_Unauthenticated_Unauthorized() throws Exception {
        // Given：配送請求
        String requestBody = """
                {
                    "deliveryMethod": "face_to_face"
                }
                """;

        // When & Then：未登入用戶嘗試設定配送  401 Unauthorized
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        // 驗證記錄未新增
        long count = shipmentRepository.count();
        assert count == 0 : "未登入用戶不應能設定配送";

        System.out.println(" TC-TR03 通過：未登入用戶無法設定配送，回傳 401 Unauthorized");
    }

    // 輔助測試 1：更新已存在的 Shipment（upsert 邏輯）
    @Test
    @DisplayName("輔助測試 1：更新已存在的 Shipment，配送方式變更正確")
    public void testUpsertShipment_UpdateExisting_Success() throws Exception {
        // Given：先建立 FACE_TO_FACE 的 Shipment
        Shipment existingShipment = Shipment.builder()
                .swapId(swap.getId())
                .senderId(userA.getId())
                .receiverIdLegacy(userB.getId())
                .deliveryMethod(Shipment.DeliveryMethod.FACE_TO_FACE)
                .build();
        existingShipment = shipmentRepository.save(existingShipment);

        // When：更新為 SHIPNOW
        String requestBody = """
                {
                    "deliveryMethod": "shipnow",
                    "trackingNumber": "TW111222333",
                    "trackingUrl": "https://track.example/TW111222333"
                }
                """;

        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(existingShipment.getId().intValue())))
                .andExpect(jsonPath("$.deliveryMethod", is("SHIPNOW")))
                .andExpect(jsonPath("$.trackingNumber", is("TW111222333")));

        // Then：驗證記錄只有一筆（upsert，不是新增）
        long count = shipmentRepository.count();
        assert count == 1 : "應該只有一筆 Shipment 記錄（upsert）";

        Shipment updatedShipment = shipmentRepository.findById(existingShipment.getId()).orElseThrow();
        assert updatedShipment.getDeliveryMethod() == Shipment.DeliveryMethod.SHIPNOW;
        assert "TW111222333".equals(updatedShipment.getTrackingNumber());

        System.out.println(" 輔助測試 1 通過：Shipment 更新成功（upsert），配送方式從 FACE_TO_FACE 變更為 SHIPNOW");
    }

    // 輔助測試 2：不分大小寫的 deliveryMethod
    @Test
    @DisplayName("輔助測試 2：deliveryMethod 不分大小寫，正確解析")
    public void testUpsertShipment_CaseInsensitiveMethod_Success() throws Exception {
        // Given：大寫的 deliveryMethod
        String requestBody = """
                {
                    "deliveryMethod": "SHIPNOW"
                }
                """;

        // When：設定配送方式
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryMethod", is("SHIPNOW")));

        // Given：混合大小寫的 deliveryMethod
        String requestBody2 = """
                {
                    "deliveryMethod": "Face_To_Face"
                }
                """;

        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryMethod", is("FACE_TO_FACE")));

        System.out.println(" 輔助測試 2 通過：deliveryMethod 不分大小寫，正確解析");
    }

    // 輔助測試 3：非 Swap 參與者嘗試設定配送
    @Test
    @DisplayName("輔助測試 3：非 Swap 參與者嘗試設定配送，回傳 403 Forbidden")
    public void testUpsertShipment_NonParticipant_Forbidden() throws Exception {
        // Given：創建第三方用戶
        User thirdPartyUser = new User();
        thirdPartyUser.setEmail("thirdparty@test.com");
        thirdPartyUser.setPasswordHash("$2a$10$TestHashC");
        thirdPartyUser.setDisplayName("Third Party");
        thirdPartyUser.setVerified(true);
        thirdPartyUser = userRepository.save(thirdPartyUser);

        MockHttpSession thirdPartySession = new MockHttpSession();
        thirdPartySession.setAttribute("userId", thirdPartyUser.getId());
        thirdPartySession.setAttribute("email", thirdPartyUser.getEmail());

        // Given：配送請求
        String requestBody = """
                {
                    "deliveryMethod": "face_to_face"
                }
                """;

        // When & Then：第三方用戶嘗試設定配送  403 Forbidden
        mockMvc.perform(post("/api/swaps/" + swap.getId() + "/shipments/my")
                        .session(thirdPartySession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden());

        System.out.println(" 輔助測試 3 通過：非 Swap 參與者無法設定配送，回傳 403 Forbidden");
    }
}