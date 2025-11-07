package com.exchange.platform.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 * 使用 STOMP 協議實現即時聊天功能
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     * /topic - 用於廣播消息（一對多）
     * /queue - 用於點對點消息（一對一）
     * /app - 客戶端發送消息的前綴
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 啟用簡單消息代理，用於將消息發送回客戶端
        config.enableSimpleBroker("/topic", "/queue");
        
        // 設置客戶端發送消息的目的地前綴
        config.setApplicationDestinationPrefixes("/app");
        
        // 設置用戶目的地前綴（用於點對點消息）
        config.setUserDestinationPrefix("/user");
    }

    /**
     * 註冊 STOMP 端點
     * 客戶端通過此端點連接 WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 註冊 WebSocket 端點
        registry.addEndpoint("/ws-chat")
                .setAllowedOriginPatterns("*")  // 允許所有來源（生產環境應限制）
                .withSockJS();  // 啟用 SockJS 作為 WebSocket 的後備選項
    }
}
