package com.exchange.platform.service;

import com.exchange.platform.entity.*;
import com.exchange.platform.exception.*;
import com.exchange.platform.repository.MessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private MessageService messageService;

    private User sender;
    private Message testMessage;

    @BeforeEach
    void setUp() {
        sender = User.builder()
                .email("sender@test.com")
                .displayName("Sender")
                .build();
        sender.setId(1L);

        testMessage = Message.builder()
                .sender(sender)
                .content("Test message")
                .isRead(false)
                .build();
        testMessage.setId(1L);
    }

    @Test
    void sendMessageForProposal_Success() {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(sender);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        Message result = messageService.sendMessageForProposal(1L, 100L, "Hello");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSender()).isEqualTo(sender);
        assertThat(result.getContent()).isEqualTo("Hello");
        assertThat(result.getIsRead()).isFalse();
        
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void sendMessageForSwap_Success() {
        // Arrange
        when(userService.getUserById(1L)).thenReturn(sender);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            msg.setId(1L);
            return msg;
        });

        // Act
        Message result = messageService.sendMessageForSwap(1L, 200L, "Swap message");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSender()).isEqualTo(sender);
        assertThat(result.getContent()).isEqualTo("Swap message");
        
        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void getProposalMessages_Success() {
        // Arrange
        when(messageRepository.findByProposalId(1L)).thenReturn(List.of(testMessage));

        // Act
        List<Message> result = messageService.getProposalMessages(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testMessage);
        
        verify(messageRepository).findByProposalId(1L);
    }

    @Test
    void getSwapMessages_Success() {
        // Arrange
        when(messageRepository.findBySwapId(1L)).thenReturn(List.of(testMessage));

        // Act
        List<Message> result = messageService.getSwapMessages(1L);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isEqualTo(testMessage);
        
        verify(messageRepository).findBySwapId(1L);
    }

    @Test
    void markAsRead_Success() {
        // Arrange
        when(messageRepository.findById(1L)).thenReturn(Optional.of(testMessage));
        when(messageRepository.save(any(Message.class))).thenReturn(testMessage);

        // Act
        Message result = messageService.markAsRead(1L, 2L);

        // Assert
        assertThat(result.getIsRead()).isTrue();
        
        verify(messageRepository).save(testMessage);
    }

    @Test
    void markAsRead_NotFound_ThrowsException() {
        // Arrange
        when(messageRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> messageService.markAsRead(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
