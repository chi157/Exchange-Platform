package com.exchange.platform.service;

import com.exchange.platform.dto.ProposalDTO;
import com.exchange.platform.dto.SwapDTO;
import com.exchange.platform.entity.Listing;
import com.exchange.platform.entity.ProposalItem;
import com.exchange.platform.entity.Shipment;
import com.exchange.platform.entity.Swap;
import com.exchange.platform.repository.ListingRepository;
import com.exchange.platform.repository.ProposalRepository;
import com.exchange.platform.repository.ShipmentRepository;
import com.exchange.platform.repository.SwapRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SwapService {

    private final SwapRepository swapRepository;
    private final ProposalRepository proposalRepository;
    private final ListingRepository listingRepository;
    private final ShipmentRepository shipmentRepository;
    private final com.exchange.platform.repository.UserRepository userRepository;
    private final ChatService chatService;
    private static final String SESSION_USER_ID = "userId";

    @Transactional(readOnly = true)
    public java.util.List<SwapDTO> listMine(HttpSession session, Integer page, Integer size, String sort) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();
        Pageable pageable = PageRequest.of(toPageIndex(page), toPageSize(size), parseSort(sort));
        Page<Swap> pg = swapRepository.findByAUserIdOrBUserId(userId, userId, pageable);
        return pg.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public SwapDTO getById(Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();
        Swap swap = swapRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) throw new ForbiddenException();
        return toDTO(swap);
    }

    public SwapDTO confirmReceived(Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(id).orElseThrow(NotFoundException::new);
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) throw new ForbiddenException();

        // Idempotent: if already completed, just return current state
        boolean isA = swap.getAUserId().equals(userId);
        if (isA) {
            if (swap.getAConfirmedAt() == null) swap.setAConfirmedAt(java.time.LocalDateTime.now());
        } else {
            if (swap.getBConfirmedAt() == null) swap.setBConfirmedAt(java.time.LocalDateTime.now());
        }

        // If both confirmed, mark completed
        if (swap.getAConfirmedAt() != null && swap.getBConfirmedAt() != null) {
            if (swap.getStatus() != Swap.Status.COMPLETED) {
                swap.setStatus(Swap.Status.COMPLETED);
                if (swap.getCompletedAt() == null) swap.setCompletedAt(java.time.LocalDateTime.now());
                // Set chat room to read-only (不會拋出異常)
                chatService.setReadOnly(swap.getId());
            }
        }

        swap = swapRepository.save(swap);
        
        if (swap.getStatus() == Swap.Status.COMPLETED) {
            finalizeListingsForCompletedSwap(swap);
        }
        
        return toDTO(swap);
    }

    public SwapDTO toDTO(Swap s) {
        // Fetch proposal with items
        final List<ProposalDTO.ProposalItemDTO>[] proposerItemsArray = new List[]{Collections.emptyList()};
        final List<ProposalDTO.ProposalItemDTO>[] receiverItemsArray = new List[]{Collections.emptyList()};
        final Long[] proposerIdArray = new Long[]{null};
        final Long[] receiverIdArray = new Long[]{null};
        
        if (s.getProposalId() != null) {
            proposalRepository.findById(s.getProposalId()).ifPresent(proposal -> {
                proposerIdArray[0] = proposal.getProposerId();
                receiverIdArray[0] = proposal.getReceiverId();
                
                proposerItemsArray[0] = proposal.getProposalItems().stream()
                        .filter(item -> item.getSide() == ProposalItem.Side.OFFERED)
                        .map(item -> {
                            Listing listing = item.getListing();
                            String display = listing.getCardName() + " - " + listing.getArtistName();
                            String imageUrl = getFirstImageUrl(listing.getImagePaths());
                            return ProposalDTO.ProposalItemDTO.builder()
                                    .itemId(item.getId())
                                    .listingId(listing.getId())
                                    .listingDisplay(display)
                                    .imageUrl(imageUrl)
                                    .side("OFFERED")
                                    .build();
                        })
                        .collect(Collectors.toList());
                
                receiverItemsArray[0] = proposal.getProposalItems().stream()
                        .filter(item -> item.getSide() == ProposalItem.Side.REQUESTED)
                        .map(item -> {
                            Listing listing = item.getListing();
                            String display = listing.getCardName() + " - " + listing.getArtistName();
                            String imageUrl = getFirstImageUrl(listing.getImagePaths());
                            return ProposalDTO.ProposalItemDTO.builder()
                                    .itemId(item.getId())
                                    .listingId(listing.getId())
                                    .listingDisplay(display)
                                    .imageUrl(imageUrl)
                                    .side("REQUESTED")
                                    .build();
                        })
                        .collect(Collectors.toList());
            });
        }
        
        // Get user display names
        String aUserDisplayName = userRepository.findById(s.getAUserId())
                .map(user -> user.getDisplayName())
                .orElse("未知使用者");
        
        String bUserDisplayName = userRepository.findById(s.getBUserId())
                .map(user -> user.getDisplayName())
                .orElse("未知使用者");
        
        String proposerDisplayName = proposerIdArray[0] != null 
                ? userRepository.findById(proposerIdArray[0])
                    .map(user -> user.getDisplayName())
                    .orElse("未知使用者")
                : null;
        
        String receiverDisplayName = receiverIdArray[0] != null
                ? userRepository.findById(receiverIdArray[0])
                    .map(user -> user.getDisplayName())
                    .orElse("未知使用者")
                : null;
        
        return SwapDTO.builder()
                .id(s.getId())
                .listingId(s.getListingId())
                .proposalId(s.getProposalId())
                .aUserId(s.getAUserId())
                .aUserDisplayName(aUserDisplayName)
                .bUserId(s.getBUserId())
                .bUserDisplayName(bUserDisplayName)
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .completedAt(s.getCompletedAt())
                .aConfirmedAt(s.getAConfirmedAt())
                .bConfirmedAt(s.getBConfirmedAt())
                .proposerItems(proposerItemsArray[0])
                .receiverItems(receiverItemsArray[0])
                .proposerId(proposerIdArray[0])
                .proposerDisplayName(proposerDisplayName)
                .receiverId(receiverIdArray[0])
                .receiverDisplayName(receiverDisplayName)
                .meetupLocation(s.getMeetupLocation())
                .meetupTime(s.getMeetupTime())
                .meetupNotes(s.getMeetupNotes())
                .aMeetupConfirmed(s.getAMeetupConfirmed())
                .bMeetupConfirmed(s.getBMeetupConfirmed())
                .deliveryMethod(s.getDeliveryMethod())
                .aDeliveryMethodConfirmed(s.getADeliveryMethodConfirmed())
                .bDeliveryMethodConfirmed(s.getBDeliveryMethodConfirmed())
                .build();
    }

            private void finalizeListingsForCompletedSwap(Swap swap) {
                java.util.Set<Long> listingIdsToComplete = new java.util.HashSet<>();
                if (swap.getListingId() != null) {
                    listingIdsToComplete.add(swap.getListingId());
                }

                if (swap.getProposalId() != null) {
                    proposalRepository.findById(swap.getProposalId()).ifPresent(proposal -> {
                        proposal.getProposalItems().stream()
                            .filter(item -> item.getSide() == ProposalItem.Side.OFFERED)
                            .map(ProposalItem::getListing)
                            .filter(java.util.Objects::nonNull)
                            .map(Listing::getId)
                            .forEach(listingIdsToComplete::add);
                    });
                }

                if (listingIdsToComplete.isEmpty()) {
                    return;
                }

                java.util.List<Listing> listings = listingRepository.findAllById(listingIdsToComplete);
                for (Listing listing : listings) {
                    if (listing.getStatus() != Listing.Status.COMPLETED) {
                        listing.setStatus(Listing.Status.COMPLETED);
                    }
                }
                listingRepository.saveAll(listings);
            }

    private int toPageIndex(Integer page) { return (page == null || page <= 1) ? 0 : page - 1; }
    private int toPageSize(Integer size) { return (size == null || size <= 0) ? 10 : Math.min(size, 100); }
    private Sort parseSort(String sort) {
        String prop = "createdAt";
        Sort.Direction dir = Sort.Direction.DESC;
        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",");
            if (parts.length >= 1 && !parts[0].isBlank()) prop = parts[0].trim();
            if (parts.length >= 2) {
                String d = parts[1].trim().toUpperCase();
                if ("ASC".equals(d)) dir = Sort.Direction.ASC; else if ("DESC".equals(d)) dir = Sort.Direction.DESC;
            }
        }
        if (!prop.equals("createdAt") && !prop.equals("updatedAt") && !prop.equals("id")) prop = "createdAt";
        return Sort.by(dir, prop);
    }

    /**
     * 從 imagePaths JSON 字串解析出第一張圖片的 URL
     */
    private String getFirstImageUrl(String imagePaths) {
        if (imagePaths == null || imagePaths.trim().isEmpty()) {
            return null;
        }
        try {
            // 簡單的JSON反序列化，移除方括號和引號
            String cleaned = imagePaths.replaceAll("[\\[\\]\"]", "");
            if (cleaned.trim().isEmpty()) {
                return null;
            }
            
            // 取得第一個檔案名稱
            String[] fileNames = cleaned.split(",");
            if (fileNames.length > 0) {
                String fileName = fileNames[0].trim();
                return "/images/" + fileName;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 設置面交資訊
     */
    @Transactional
    public SwapDTO setMeetupInfo(Long swapId, String location, LocalDateTime time, String notes, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        
        // 驗證權限：只有參與者可以設置
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) {
            throw new ForbiddenException();
        }

        // 獲取設置者的顯示名稱
        String userName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("使用者");
        
        // 判斷是新增還是修改
        boolean isNewMeetup = (swap.getMeetupLocation() == null || swap.getMeetupTime() == null);
        
        swap.setMeetupLocation(location);
        swap.setMeetupTime(time);
        swap.setMeetupNotes(notes);
        
        // 重置雙方確認狀態（因為資訊有變更）
        swap.setAMeetupConfirmed(false);
        swap.setBMeetupConfirmed(false);
        
        swap = swapRepository.save(swap);
        
        // 發送聊天室系統消息
        try {
            String timeStr = time.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
            String message;
            if (isNewMeetup) {
                message = String.format("📍 %s 設置了面交資訊：\n地點：%s\n時間：%s", 
                    userName, location, timeStr);
            } else {
                message = String.format("📍 %s 修改了面交資訊：\n地點：%s\n時間：%s\n⚠️ 請雙方重新確認", 
                    userName, location, timeStr);
            }
            if (notes != null && !notes.trim().isEmpty()) {
                message += "\n備註：" + notes;
            }
            
            // 通過 ChatService 發送系統消息（不會拋出異常）
            chatService.sendMeetupSystemMessage(swapId, message);
        } catch (Exception e) {
            // 記錄錯誤但不影響面交資訊保存
            System.err.println("Failed to send meetup system message: " + e.getMessage());
        }
        
        return toDTO(swap);
    }

    /**
     * 確認面交資訊
     */
    @Transactional
    public SwapDTO confirmMeetup(Long swapId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        
        // 驗證權限
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) {
            throw new ForbiddenException();
        }

        // 檢查是否已設置面交資訊
        if (swap.getMeetupLocation() == null || swap.getMeetupTime() == null) {
            throw new IllegalStateException("尚未設置面交資訊");
        }

        // 獲取確認者的顯示名稱
        String userName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("使用者");

        // 設置對應用戶的確認狀態
        boolean isA = swap.getAUserId().equals(userId);
        if (isA) {
            swap.setAMeetupConfirmed(true);
        } else {
            swap.setBMeetupConfirmed(true);
        }

        swap = swapRepository.save(swap);
        
        // 確認面交資訊時，也創建該用戶的 Shipment 記錄（如果尚未創建）
        // 這樣 loadOtherShipment() 才能正確檢測到對方已設定物流資訊
        java.util.Optional<Shipment> existingShipment = shipmentRepository.findBySwapIdAndSenderId(swapId, userId);
        if (existingShipment.isEmpty()) {
            System.out.println("[confirmMeetup] Creating new Shipment for user " + userId + " on swap " + swapId);
            Shipment shipment = new Shipment();
            shipment.setSwapId(swapId);
            shipment.setSenderId(userId);
            shipment.setDeliveryMethod(Shipment.DeliveryMethod.FACE_TO_FACE);
            shipment.setCreatedAt(java.time.LocalDateTime.now());
            shipmentRepository.save(shipment);
            System.out.println("[confirmMeetup] Successfully created Shipment for user " + userId);
        } else {
            System.out.println("[confirmMeetup] Shipment already exists for user " + userId + " on swap " + swapId);
        }
        
        // 發送聊天室系統消息（不會拋出異常）
        String message;
        // 檢查是否雙方都已確認
        if (swap.getAMeetupConfirmed() != null && swap.getAMeetupConfirmed() 
            && swap.getBMeetupConfirmed() != null && swap.getBMeetupConfirmed()) {
            message = "✅ 雙方已確認面交資訊！可以準備進行面交了。";
        } else {
            message = String.format("✅ %s 已確認面交資訊", userName);
        }
        chatService.sendMeetupSystemMessage(swapId, message);
        
        return toDTO(swap);
    }

    /**
     * 提議配送方式（面交或交貨便）
     */
    @Transactional
    public SwapDTO proposeDeliveryMethod(Long swapId, String method, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        
        // 驗證權限
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) {
            throw new ForbiddenException();
        }

        // 驗證配送方式
        if (!"FACE_TO_FACE".equals(method) && !"SHIPNOW".equals(method)) {
            throw new IllegalArgumentException("無效的配送方式");
        }

        // 獲取提議者的顯示名稱
        String userName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("使用者");

        // 設置配送方式並重置確認狀態
        swap.setDeliveryMethod(method);
        
        // 設置提議者為已確認，另一方為未確認
        boolean isA = swap.getAUserId().equals(userId);
        if (isA) {
            swap.setADeliveryMethodConfirmed(true);
            swap.setBDeliveryMethodConfirmed(false);
        } else {
            swap.setADeliveryMethodConfirmed(false);
            swap.setBDeliveryMethodConfirmed(true);
        }

        swap = swapRepository.save(swap);
        
        // 發送聊天室系統消息（不會拋出異常）
        String methodText = "FACE_TO_FACE".equals(method) ? "面交" : "交貨便";
        String message = String.format("📋 %s 提議使用「%s」作為配送方式，等待對方確認", userName, methodText);
        chatService.sendMeetupSystemMessage(swapId, message);
        
        return toDTO(swap);
    }

    /**
     * 確認配送方式
     */
    @Transactional
    public SwapDTO confirmDeliveryMethod(Long swapId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        
        // 驗證權限
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) {
            throw new ForbiddenException();
        }

        // 檢查是否已有提議的配送方式
        if (swap.getDeliveryMethod() == null) {
            throw new IllegalStateException("尚未提議配送方式");
        }

        // 獲取確認者的顯示名稱
        String userName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("使用者");

        // 設置對應用戶的確認狀態
        boolean isA = swap.getAUserId().equals(userId);
        if (isA) {
            swap.setADeliveryMethodConfirmed(true);
        } else {
            swap.setBDeliveryMethodConfirmed(true);
        }

        swap = swapRepository.save(swap);
        
        // 發送聊天室系統消息（不會拋出異常）
        String message;
        // 檢查是否雙方都已確認
        if (Boolean.TRUE.equals(swap.getADeliveryMethodConfirmed()) 
            && Boolean.TRUE.equals(swap.getBDeliveryMethodConfirmed())) {
            String methodText = "FACE_TO_FACE".equals(swap.getDeliveryMethod()) ? "面交" : "交貨便";
            message = String.format("✅ 雙方已確認使用「%s」作為配送方式！", methodText);
        } else {
            message = String.format("✅ %s 已同意配送方式", userName);
        }
        chatService.sendMeetupSystemMessage(swapId, message);
        
        return toDTO(swap);
    }

    /**
     * 拒絕配送方式（清空並重新開始）
     */
    @Transactional
    public SwapDTO rejectDeliveryMethod(Long swapId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) throw new UnauthorizedException();

        Swap swap = swapRepository.findById(swapId).orElseThrow(NotFoundException::new);
        
        // 驗證權限
        if (!swap.getAUserId().equals(userId) && !swap.getBUserId().equals(userId)) {
            throw new ForbiddenException();
        }

        // 獲取拒絕者的顯示名稱
        String userName = userRepository.findById(userId)
                .map(user -> user.getDisplayName())
                .orElse("使用者");

        // 清空配送方式相關設定
        swap.setDeliveryMethod(null);
        swap.setADeliveryMethodConfirmed(false);
        swap.setBDeliveryMethodConfirmed(false);

        swap = swapRepository.save(swap);
        
        // 發送聊天室系統消息（不會拋出異常）
        String message = String.format("❌ %s 不同意此配送方式，請重新協商", userName);
        chatService.sendMeetupSystemMessage(swapId, message);
        
        return toDTO(swap);
    }

    public static class UnauthorizedException extends RuntimeException {}
    public static class NotFoundException extends RuntimeException {}
    public static class ForbiddenException extends RuntimeException {}
    public static class ConflictException extends RuntimeException {}
}
