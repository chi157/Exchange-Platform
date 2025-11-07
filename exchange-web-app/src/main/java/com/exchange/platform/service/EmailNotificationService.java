package com.exchange.platform.service;

import com.exchange.platform.entity.*;
import com.exchange.platform.entity.EmailNotification.NotificationType;
import com.exchange.platform.repository.EmailNotificationRepository;
import com.exchange.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final EmailNotificationRepository emailNotificationRepository;
    private final JavaMailSender javaMailSender;
    private final UserRepository userRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * 創建並發送電子郵件通知
     */
    @Transactional
    public void createAndSendNotification(Long recipientId, NotificationType type, 
                                        String relatedEntityType, Long relatedEntityId, 
                                        Object... contentParams) {
        try {
            Optional<User> recipientOpt = userRepository.findById(recipientId);
            if (recipientOpt.isEmpty() || recipientOpt.get().getEmail() == null) {
                log.warn("無法發送通知：收件人 {} 不存在或沒有電子郵件", recipientId);
                return;
            }

            User recipient = recipientOpt.get();

            // 檢查是否已經發送過相同通知（避免重複）
            List<EmailNotification> recentNotifications = emailNotificationRepository
                    .findRecentNotificationsByTypeAndEntity(recipientId, type, relatedEntityId, 
                            PageRequest.of(0, 1));
            
            if (!recentNotifications.isEmpty()) {
                EmailNotification recent = recentNotifications.get(0);
                // 如果5分鐘內已發送相同通知，則跳過
                if (recent.getCreatedAt().isAfter(LocalDateTime.now().minusMinutes(5))) {
                    log.info("跳過重複通知：{} for 用戶 {} 實體 {}", type, recipientId, relatedEntityId);
                    return;
                }
            }

            String subject = generateSubject(type, contentParams);
            String content = generateContent(type, relatedEntityType, relatedEntityId, contentParams);

            EmailNotification notification = EmailNotification.builder()
                    .recipientId(recipientId)
                    .email(recipient.getEmail())
                    .notificationType(type)
                    .subject(subject)
                    .content(content)
                    .relatedEntityType(relatedEntityType)
                    .relatedEntityId(relatedEntityId)
                    .sent(false)
                    .build();

            emailNotificationRepository.save(notification);
            sendEmailAsync(notification);

        } catch (Exception e) {
            log.error("創建通知時發生錯誤：{}", e.getMessage(), e);
        }
    }

    /**
     * 異步發送電子郵件
     */
    @Async
    public void sendEmailAsync(EmailNotification notification) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(notification.getEmail());
            message.setSubject(notification.getSubject());
            message.setText(notification.getContent());

            javaMailSender.send(message);

            // 標記為已發送
            notification.setSent(true);
            notification.setSentAt(LocalDateTime.now());
            emailNotificationRepository.save(notification);

            log.info("電子郵件通知已發送：{} to {}", notification.getNotificationType(), notification.getEmail());

        } catch (Exception e) {
            log.error("發送電子郵件失敗：{}", e.getMessage(), e);
            // 保持 sent = false，以便稍後重試
        }
    }

    /**
     * 重新發送失敗的通知
     */
    @Transactional
    public void retryFailedNotifications() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
        List<EmailNotification> failedNotifications = 
                emailNotificationRepository.findFailedNotificationsForRetry(cutoffTime);

        for (EmailNotification notification : failedNotifications) {
            sendEmailAsync(notification);
        }

        log.info("重試 {} 個失敗的電子郵件通知", failedNotifications.size());
    }

    /**
     * 生成郵件主題
     */
    private String generateSubject(NotificationType type, Object... params) {
        switch (type) {
            case PROPOSAL_RECEIVED:
                return "【卡片交換平台】您有新的交換提案！";
            case PROPOSAL_ACCEPTED:
                return "【卡片交換平台】您的提案已被接受！";
            case PROPOSAL_REJECTED:
                return "【卡片交換平台】提案狀態更新";
            case SWAP_CONFIRMED:
                return "【卡片交換平台】交換確認成功！";
            case DELIVERY_METHOD_PROPOSED:
                return "【卡片交換平台】運送方式提案";
            case DELIVERY_METHOD_ACCEPTED:
                return "【卡片交換平台】運送方式已確認";
            case SHIPMENT_SENT:
                return "【卡片交換平台】包裹已寄出";
            case SHIPMENT_RECEIVED:
                return "【卡片交換平台】包裹已送達";
            case EXCHANGE_COMPLETED:
                return "【卡片交換平台】交換已完成！";
            default:
                return "【卡片交換平台】通知";
        }
    }

    /**
     * 生成郵件內容
     */
    private String generateContent(NotificationType type, String entityType, Long entityId, Object... params) {
        StringBuilder content = new StringBuilder();
        content.append("親愛的用戶，您好！\n\n");

        switch (type) {
            case PROPOSAL_RECEIVED:
                content.append("您收到了一個新的卡片交換提案！\n");
                content.append("提案編號：").append(entityId).append("\n");
                content.append("請登入平台查看詳細內容並回應提案。\n");
                break;

            case PROPOSAL_ACCEPTED:
                content.append("恭喜！您的交換提案已被接受！\n");
                content.append("提案編號：").append(entityId).append("\n");
                content.append("請登入平台查看交換詳情並進行下一步操作。\n");
                break;

            case PROPOSAL_REJECTED:
                content.append("您的交換提案已被拒絕。\n");
                content.append("提案編號：").append(entityId).append("\n");
                content.append("請登入平台查看詳情或提出新的提案。\n");
                break;

            case SWAP_CONFIRMED:
                content.append("交換已確認！\n");
                content.append("交換編號：").append(entityId).append("\n");
                content.append("請與交換夥伴確認運送方式。\n");
                break;

            case DELIVERY_METHOD_PROPOSED:
                content.append("對方已提出運送方式建議。\n");
                content.append("交換編號：").append(entityId).append("\n");
                content.append("請登入平台確認運送安排。\n");
                break;

            case DELIVERY_METHOD_ACCEPTED:
                content.append("運送方式已確認！\n");
                content.append("交換編號：").append(entityId).append("\n");
                content.append("請按照約定的方式進行運送。\n");
                break;

            case SHIPMENT_SENT:
                content.append("您的包裹已寄出！\n");
                content.append("交換編號：").append(entityId).append("\n");
                if (params.length > 0) {
                    content.append("追蹤號碼：").append(params[0]).append("\n");
                }
                content.append("請登入平台查看物流狀態。\n");
                break;

            case SHIPMENT_RECEIVED:
                content.append("包裹已送達！\n");
                content.append("交換編號：").append(entityId).append("\n");
                content.append("請確認收到物品並完成交換確認。\n");
                break;

            case EXCHANGE_COMPLETED:
                content.append("交換已完成！\n");
                content.append("交換編號：").append(entityId).append("\n");
                content.append("歡迎為本次交換留下評價，謝謝您的使用！\n");
                break;

            default:
                content.append("您有新的通知，請登入平台查看。\n");
                if (entityId != null) {
                    content.append("相關編號：").append(entityId).append("\n");
                }
        }

        content.append("\n");
        content.append("點擊這裡登入平台：").append(baseUrl).append("\n\n");
        content.append("卡片交換平台團隊\n");
        content.append("發送時間：").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        return content.toString();
    }

    /**
     * 便民方法：為提案相關事件發送通知
     */
    public void sendProposalNotification(Proposal proposal, NotificationType type, Long recipientId) {
        createAndSendNotification(recipientId, type, "Proposal", proposal.getId());
    }

    /**
     * 便民方法：為交換相關事件發送通知
     */
    public void sendSwapNotification(Swap swap, NotificationType type, Long recipientId) {
        createAndSendNotification(recipientId, type, "Swap", swap.getId());
    }

    /**
     * 便民方法：為物流相關事件發送通知
     */
    public void sendShipmentNotification(Shipment shipment, NotificationType type, Long recipientId, String... extraParams) {
        createAndSendNotification(recipientId, type, "Shipment", shipment.getId(), (Object[]) extraParams);
    }
}