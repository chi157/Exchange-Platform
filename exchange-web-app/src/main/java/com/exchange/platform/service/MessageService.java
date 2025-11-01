package com.exchange.platform.service;

import com.exchange.platform.entity.Message;
import com.exchange.platform.entity.User;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserService userService;

    public Message sendMessageForProposal(Long senderId, Long proposalId, String content) {
        User sender = userService.getUserById(senderId);
        
        Message message = Message.builder()
                .sender(sender)
                .content(content)
                .isRead(false)
                .build();
        
        return messageRepository.save(message);
    }

    public Message sendMessageForSwap(Long senderId, Long swapId, String content) {
        User sender = userService.getUserById(senderId);
        
        Message message = Message.builder()
                .sender(sender)
                .content(content)
                .isRead(false)
                .build();
        
        return messageRepository.save(message);
    }

    @Transactional(readOnly = true)
    public List<Message> getProposalMessages(Long proposalId) {
        return messageRepository.findByProposalId(proposalId);
    }

    @Transactional(readOnly = true)
    public List<Message> getSwapMessages(Long swapId) {
        return messageRepository.findBySwapId(swapId);
    }

    public Message markAsRead(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message", messageId));
        
        message.markAsRead();
        return messageRepository.save(message);
    }
}
