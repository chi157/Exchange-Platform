package com.exchange.platform.service;

import com.exchange.platform.entity.User;
import com.exchange.platform.exception.ResourceNotFoundException;
import com.exchange.platform.exception.ValidationException;
import com.exchange.platform.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .displayName("Test User")
                .passwordHash("hashedPassword")
                .verified(true)
                .riskScore(0)
                .isBlacklisted(false)
                .build();
        testUser.setId(1L);
        testUser.addRole("USER");
    }

    @Test
    void registerUser_Success() {
        // Arrange
        String email = "newuser@example.com";
        String password = "password123";
        String displayName = "New User";
        
        when(userRepository.existsByEmail(email)).thenReturn(false);
        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });

        // Act
        User result = userService.registerUser(email, password, displayName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getDisplayName()).isEqualTo(displayName);
        assertThat(result.getPasswordHash()).isEqualTo("encodedPassword");
        assertThat(result.getVerified()).isFalse();
        assertThat(result.getVerificationToken()).isNotNull();
        assertThat(result.getRoles()).contains("USER");
        
        verify(userRepository).existsByEmail(email);
        verify(passwordEncoder).encode(password);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_EmailAlreadyExists_ThrowsValidationException() {
        // Arrange
        String email = "existing@example.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> userService.registerUser(email, "password", "User"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User registration failed");
        
        verify(userRepository).existsByEmail(email);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserById_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserById(1L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).findById(1L);
    }

    @Test
    void getUserById_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        
        verify(userRepository).findById(999L);
    }

    @Test
    void getUserByEmail_Success() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByEmail("test@example.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("test@example.com");
        
        verify(userRepository).findByEmail("test@example.com");
    }

    @Test
    void getUserByEmail_NotFound_ThrowsResourceNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userService.getUserByEmail("notfound@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with email");
        
        verify(userRepository).findByEmail("notfound@example.com");
    }

    @Test
    void getHighRiskUsers_Success() {
        // Arrange
        User highRiskUser1 = User.builder()
                .email("risky1@example.com")
                .riskScore(75)
                .build();
        highRiskUser1.setId(2L);
        
        User highRiskUser2 = User.builder()
                .email("risky2@example.com")
                .riskScore(90)
                .build();
        highRiskUser2.setId(3L);
        
        List<User> highRiskUsers = Arrays.asList(highRiskUser1, highRiskUser2);
        when(userRepository.findHighRiskUsers(50)).thenReturn(highRiskUsers);

        // Act
        List<User> result = userService.getHighRiskUsers(50);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).extracting(User::getRiskScore).containsExactly(75, 90);
        
        verify(userRepository).findHighRiskUsers(50);
    }
}
