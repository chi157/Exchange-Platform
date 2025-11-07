package com.exchange.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomListDTO {
    private Long id;
    private Long proposalId;
    private Long swapId;
    private String otherUserName;
    private String itemsSummary;
    private Long unreadCount;
    private LocalDateTime lastMessageAt;
    private String status;
}
