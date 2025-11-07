package com.exchange.platform.service;

import com.exchange.platform.entity.*;
import com.exchange.platform.entity.EmailNotification.NotificationType;
import com.exchange.platform.repository.EmailNotificationRepository;
import com.exchange.platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.internet.MimeMessage;
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

    @Value("${spring.mail.from-name:卡片交換平台}")
    private String fromName;

    @Value("${spring.mail.from-email:${spring.mail.username}}")
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
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(notification.getEmail());
            helper.setSubject(notification.getSubject());
            helper.setText(notification.getContent(), true); // true = HTML

            javaMailSender.send(mimeMessage);

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
     * 生成郵件內容（HTML 格式）
     */
    private String generateContent(NotificationType type, String entityType, Long entityId, Object... params) {
        String icon = getNotificationIcon(type);
        String title = getNotificationTitle(type);
        String message = getNotificationMessage(type, entityId, params);
        
        return generateHtmlTemplate(icon, title, message, entityId);
    }

    /**
     * 獲取通知圖標
     */
    private String getNotificationIcon(NotificationType type) {
        switch (type) {
            case PROPOSAL_RECEIVED: return "📨";
            case PROPOSAL_ACCEPTED: return "✅";
            case PROPOSAL_REJECTED: return "❌";
            case SWAP_CONFIRMED: return "🔄";
            case DELIVERY_METHOD_PROPOSED: return "📋";
            case DELIVERY_METHOD_ACCEPTED: return "✅";
            case SHIPMENT_SENT: return "📦";
            case SHIPMENT_RECEIVED: return "📬";
            case EXCHANGE_COMPLETED: return "🎉";
            default: return "📢";
        }
    }

    /**
     * 獲取通知標題
     */
    private String getNotificationTitle(NotificationType type) {
        switch (type) {
            case PROPOSAL_RECEIVED: return "您收到了一個新的交換提案！";
            case PROPOSAL_ACCEPTED: return "恭喜！您的提案已被接受！";
            case PROPOSAL_REJECTED: return "提案狀態更新";
            case SWAP_CONFIRMED: return "交換確認成功！";
            case DELIVERY_METHOD_PROPOSED: return "運送方式提案";
            case DELIVERY_METHOD_ACCEPTED: return "運送方式已確認！";
            case SHIPMENT_SENT: return "包裹已寄出！";
            case SHIPMENT_RECEIVED: return "包裹已送達！";
            case EXCHANGE_COMPLETED: return "交換完成！";
            default: return "平台通知";
        }
    }

    /**
     * 獲取通知訊息內容
     */
    private String getNotificationMessage(NotificationType type, Long entityId, Object... params) {
        StringBuilder msg = new StringBuilder();
        
        switch (type) {
            case PROPOSAL_RECEIVED:
                msg.append("<p>有用戶對您的卡片感興趣，向您提出了交換提案！</p>");
                msg.append("<p><strong>提案編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>請登入平台查看詳細內容並回應提案。</p>");
                break;

            case PROPOSAL_ACCEPTED:
                msg.append("<p>您的交換提案已被對方接受！</p>");
                msg.append("<p><strong>提案編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>接下來請與對方協商配送方式，完成交換流程。</p>");
                break;

            case PROPOSAL_REJECTED:
                msg.append("<p>您的交換提案已被拒絕。</p>");
                msg.append("<p><strong>提案編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>別灰心！您可以重新選擇其他卡片提出新的提案。</p>");
                break;

            case SWAP_CONFIRMED:
                msg.append("<p>交換已確認成功！</p>");
                msg.append("<p><strong>交換編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>請與交換夥伴確認配送方式（面交或交貨便）。</p>");
                break;

            case DELIVERY_METHOD_PROPOSED:
                msg.append("<p>對方已提出配送方式建議，等待您的確認。</p>");
                msg.append("<p><strong>交換編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>請盡快登入平台查看並回應配送安排。</p>");
                break;

            case DELIVERY_METHOD_ACCEPTED:
                msg.append("<p>雙方已確認配送方式！</p>");
                msg.append("<p><strong>交換編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>請按照約定的方式進行配送，並記得更新物流資訊。</p>");
                break;

            case SHIPMENT_SENT:
                msg.append("<p>對方已將包裹寄出！</p>");
                msg.append("<p><strong>交換編號：</strong>#").append(entityId).append("</p>");
                if (params.length > 0 && params[0] != null) {
                    msg.append("<p><strong>追蹤號碼：</strong>").append(params[0]).append("</p>");
                }
                msg.append("<p>您可以登入平台查看物流狀態。</p>");
                break;

            case SHIPMENT_RECEIVED:
                msg.append("<p>包裹已送達指定地點！</p>");
                msg.append("<p><strong>交換編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>請確認收到物品後，完成交換確認。</p>");
                break;

            case EXCHANGE_COMPLETED:
                msg.append("<p>恭喜！交換已順利完成！</p>");
                msg.append("<p><strong>交換編號：</strong>#").append(entityId).append("</p>");
                msg.append("<p>歡迎為本次交換留下評價，幫助其他用戶更了解交換夥伴。</p>");
                break;

            default:
                msg.append("<p>您有新的平台通知，請登入查看詳情。</p>");
                if (entityId != null) {
                    msg.append("<p><strong>相關編號：</strong>#").append(entityId).append("</p>");
                }
        }
        
        return msg.toString();
    }

    /**
     * 生成 HTML 郵件模板
     */
    private String generateHtmlTemplate(String icon, String title, String message, Long entityId) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
        
        return "<!DOCTYPE html>" +
                "<html lang='zh-TW'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>卡片交換平台通知</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: \"Microsoft JhengHei\", \"Segoe UI\", Arial, sans-serif; background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%); padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background: white; border-radius: 16px; box-shadow: 0 8px 32px rgba(106, 0, 214, 0.15); overflow: hidden;'>" +
                "                    <!-- Header -->" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%); padding: 30px 40px; text-align: center;'>" +
                "                            <div style='font-size: 48px; margin-bottom: 10px;'>" + icon + "</div>" +
                "                            <h1 style='margin: 0; color: white; font-size: 28px; font-weight: 700;'>卡片交換平台</h1>" +
                "                            <p style='margin: 10px 0 0 0; color: rgba(255,255,255,0.9); font-size: 14px;'>Exchange Platform</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <!-- Content -->" +
                "                    <tr>" +
                "                        <td style='padding: 40px;'>" +
                "                            <h2 style='margin: 0 0 20px 0; color: #7c3aed; font-size: 22px; font-weight: 700; border-bottom: 3px solid #e9d5ff; padding-bottom: 12px;'>" + title + "</h2>" +
                "                            <div style='color: #374151; font-size: 16px; line-height: 1.8;'>" +
                message +
                "                            </div>" +
                "                            <!-- Action Button -->" +
                "                            <div style='text-align: center; margin: 30px 0;'>" +
                "                                <a href='" + baseUrl + "' style='display: inline-block; background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%); color: white; text-decoration: none; padding: 14px 32px; border-radius: 12px; font-weight: 700; font-size: 16px; box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);'>🔗 前往平台查看</a>" +
                "                            </div>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <!-- Footer -->" +
                "                    <tr>" +
                "                        <td style='background: #f9fafb; padding: 30px 40px; border-top: 2px solid #e5e7eb;'>" +
                "                            <p style='margin: 0 0 10px 0; color: #6b7280; font-size: 13px; text-align: center;'>📧 此郵件由系統自動發送，請勿直接回覆</p>" +
                "                            <p style='margin: 0 0 10px 0; color: #6b7280; font-size: 13px; text-align: center;'>⏰ 發送時間：" + currentTime + "</p>" +
                "                            <p style='margin: 0; color: #6b7280; font-size: 13px; text-align: center;'>© 2025 卡片交換平台 Exchange Platform. All rights reserved.</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
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

    /**
     * 發送驗證碼郵件（用於註冊和變更郵箱）
     */
    @Async
    public void sendVerificationCode(String email, String verificationCode, String purpose) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(email);
            
            String subject;
            String icon;
            String title;
            String message;
            
            if ("REGISTER".equals(purpose)) {
                subject = "【卡片交換平台】歡迎註冊 - 請驗證您的電子郵件";
                icon = "🎉";
                title = "歡迎加入卡片交換平台！";
                message = "<p>感謝您註冊卡片交換平台！</p>" +
                         "<p>為了確保您的帳號安全，請使用以下驗證碼完成註冊：</p>" +
                         "<div style='text-align: center; margin: 30px 0;'>" +
                         "    <div style='display: inline-block; background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%); color: white; padding: 20px 40px; border-radius: 12px; font-size: 32px; font-weight: 700; letter-spacing: 8px; box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);'>" +
                         verificationCode +
                         "    </div>" +
                         "</div>" +
                         "<p style='color: #dc2626; font-weight: 600;'>⏰ 驗證碼有效時間：10分鐘</p>" +
                         "<p style='color: #6b7280; font-size: 14px;'>💡 如果您沒有註冊此帳號，請忽略此郵件。</p>";
            } else {
                subject = "【卡片交換平台】電子郵件變更驗證";
                icon = "🔐";
                title = "電子郵件變更驗證";
                message = "<p>您正在變更您的電子郵件地址。</p>" +
                         "<p>為了確保您的帳號安全，請使用以下驗證碼完成變更：</p>" +
                         "<div style='text-align: center; margin: 30px 0;'>" +
                         "    <div style='display: inline-block; background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%); color: white; padding: 20px 40px; border-radius: 12px; font-size: 32px; font-weight: 700; letter-spacing: 8px; box-shadow: 0 4px 12px rgba(124, 58, 237, 0.3);'>" +
                         verificationCode +
                         "    </div>" +
                         "</div>" +
                         "<p style='color: #dc2626; font-weight: 600;'>⏰ 驗證碼有效時間：10分鐘</p>" +
                         "<p style='color: #6b7280; font-size: 14px;'>💡 如果您沒有進行此操作，請立即聯繫客服或變更密碼。</p>";
            }
            
            helper.setSubject(subject);
            helper.setText(generateVerificationEmailTemplate(icon, title, message), true);

            javaMailSender.send(mimeMessage);
            log.info("驗證碼郵件已發送：{} to {}", purpose, email);

        } catch (Exception e) {
            log.error("發送驗證碼郵件失敗：{}", e.getMessage(), e);
        }
    }

    /**
     * 生成驗證碼郵件的 HTML 模板
     */
    private String generateVerificationEmailTemplate(String icon, String title, String message) {
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
        
        return "<!DOCTYPE html>" +
                "<html lang='zh-TW'>" +
                "<head>" +
                "    <meta charset='UTF-8'>" +
                "    <meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "    <title>卡片交換平台 - 驗證碼</title>" +
                "</head>" +
                "<body style='margin: 0; padding: 0; font-family: \"Microsoft JhengHei\", \"Segoe UI\", Arial, sans-serif; background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%);'>" +
                "    <table width='100%' cellpadding='0' cellspacing='0' style='background: linear-gradient(135deg, #f5f3ff 0%, #ede9fe 100%); padding: 40px 20px;'>" +
                "        <tr>" +
                "            <td align='center'>" +
                "                <table width='600' cellpadding='0' cellspacing='0' style='background: white; border-radius: 16px; box-shadow: 0 8px 32px rgba(106, 0, 214, 0.15); overflow: hidden;'>" +
                "                    <!-- Header -->" +
                "                    <tr>" +
                "                        <td style='background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%); padding: 30px 40px; text-align: center;'>" +
                "                            <div style='font-size: 48px; margin-bottom: 10px;'>" + icon + "</div>" +
                "                            <h1 style='margin: 0; color: white; font-size: 28px; font-weight: 700;'>卡片交換平台</h1>" +
                "                            <p style='margin: 10px 0 0 0; color: rgba(255,255,255,0.9); font-size: 14px;'>Exchange Platform</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <!-- Content -->" +
                "                    <tr>" +
                "                        <td style='padding: 40px;'>" +
                "                            <h2 style='margin: 0 0 20px 0; color: #7c3aed; font-size: 22px; font-weight: 700; border-bottom: 3px solid #e9d5ff; padding-bottom: 12px;'>" + title + "</h2>" +
                "                            <div style='color: #374151; font-size: 16px; line-height: 1.8;'>" +
                message +
                "                            </div>" +
                "                        </td>" +
                "                    </tr>" +
                "                    <!-- Footer -->" +
                "                    <tr>" +
                "                        <td style='background: #f9fafb; padding: 30px 40px; border-top: 2px solid #e5e7eb;'>" +
                "                            <p style='margin: 0 0 10px 0; color: #6b7280; font-size: 13px; text-align: center;'>📧 此郵件由系統自動發送，請勿直接回覆</p>" +
                "                            <p style='margin: 0 0 10px 0; color: #6b7280; font-size: 13px; text-align: center;'>⏰ 發送時間：" + currentTime + "</p>" +
                "                            <p style='margin: 0; color: #6b7280; font-size: 13px; text-align: center;'>© 2025 卡片交換平台 Exchange Platform. All rights reserved.</p>" +
                "                        </td>" +
                "                    </tr>" +
                "                </table>" +
                "            </td>" +
                "        </tr>" +
                "    </table>" +
                "</body>" +
                "</html>";
    }
}