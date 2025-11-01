// package com.exchange.platform.service;

// import com.exchange.platform.dto.AuthResponse;
// import com.exchange.platform.dto.LoginRequest;
// import com.exchange.platform.dto.RegisterRequest;
// import com.exchange.platform.dto.UserDTO;
// import com.exchange.platform.entity.User;
// import com.exchange.platform.exception.BusinessRuleViolationException;
// import com.exchange.platform.repository.UserRepository;
// import jakarta.servlet.http.HttpSession;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;
// import org.springframework.security.authentication.AuthenticationManager;
// import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContext;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.security.crypto.password.PasswordEncoder;

// import java.util.HashSet;
// import java.util.Optional;
// import java.util.Set;

// import static org.assertj.core.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.*;
// import static org.mockito.Mockito.*;

// @ExtendWith(MockitoExtension.class)
// class AuthServiceTest {

//     @Mock
//     private UserRepository userRepository;

//     @Mock
//     private PasswordEncoder passwordEncoder;

//     @Mock
//     private AuthenticationManager authenticationManager;

//     @Mock
//     private HttpSession session;

//     @Mock
//     private Authentication authentication;

//     @Mock
//     private SecurityContext securityContext;

//     @InjectMocks
//     private AuthService authService;

//     private User testUser;
//     private RegisterRequest registerRequest;
//     private LoginRequest loginRequest;

//     @BeforeEach
//     void setUp() {
//         // Setup test user
//         Set<String> roles = new HashSet<>();
//         roles.add("USER");
        
//         testUser = User.builder()
//                 .email("test@example.com")
//                 .passwordHash("$2a$10$hashedPassword")
//                 .displayName("Test User")
//                 .verified(false)
//                 .roles(roles)
//                 .riskScore(0)
//                 .isBlacklisted(false)
//                 .build();
//         testUser.setId(1L);

//         // Setup request objects
//         registerRequest = RegisterRequest.builder()
//                 .email("newuser@example.com")
//                 .password("password123")
//                 .displayName("New User")
//                 .build();

//         loginRequest = LoginRequest.builder()
//                 .email("test@example.com")
//                 .password("password123")
//                 .build();
//     }

//     @Test
//     void register_Success() {
//         // Arrange
//         when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
//         when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("$2a$10$encodedPassword");
//         when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
//             User user = invocation.getArgument(0);
//             user.setId(2L);
//             return user;
//         });

//         // Act
//         AuthResponse response = authService.register(registerRequest);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getSuccess()).isTrue();
//         assertThat(response.getEmail()).isEqualTo(registerRequest.getEmail());
//         assertThat(response.getDisplayName()).isEqualTo(registerRequest.getDisplayName());
//         assertThat(response.getUserId()).isEqualTo(2L);
//         assertThat(response.getMessage()).isEqualTo("Registration successful");

//         verify(userRepository).findByEmail(registerRequest.getEmail());
//         verify(passwordEncoder).encode(registerRequest.getPassword());
//         verify(userRepository).save(any(User.class));
//     }

//     @Test
//     void register_EmailAlreadyExists_ThrowsException() {
//         // Arrange
//         when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.of(testUser));

//         // Act & Assert
//         assertThatThrownBy(() -> authService.register(registerRequest))
//                 .isInstanceOf(BusinessRuleViolationException.class)
//                 .hasMessage("Email already registered");

//         verify(userRepository).findByEmail(registerRequest.getEmail());
//         verify(passwordEncoder, never()).encode(anyString());
//         verify(userRepository, never()).save(any(User.class));
//     }

//     @Test
//     void register_CreatesUserWithDefaultRole() {
//         // Arrange
//         when(userRepository.findByEmail(registerRequest.getEmail())).thenReturn(Optional.empty());
//         when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encodedPassword");
//         when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
//             User user = invocation.getArgument(0);
//             user.setId(2L);
//             return user;
//         });

//         // Act
//         authService.register(registerRequest);

//         // Assert
//         verify(userRepository).save(argThat(user -> 
//             user.getRoles() != null && 
//             user.getRoles().contains("USER") &&
//             user.getVerified() == false &&
//             user.getRiskScore() == 0 &&
//             user.getIsBlacklisted() == false
//         ));
//     }

//     @Test
//     void login_Success() {
//         // Arrange
//         when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                 .thenReturn(authentication);
//         when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));

//         // Mock SecurityContextHolder
//         SecurityContextHolder.setContext(securityContext);

//         // Act
//         AuthResponse response = authService.login(loginRequest, session);

//         // Assert
//         assertThat(response).isNotNull();
//         assertThat(response.getSuccess()).isTrue();
//         assertThat(response.getEmail()).isEqualTo(testUser.getEmail());
//         assertThat(response.getDisplayName()).isEqualTo(testUser.getDisplayName());
//         assertThat(response.getUserId()).isEqualTo(testUser.getId());
//         assertThat(response.getMessage()).isEqualTo("Login successful");

//         verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
//         verify(userRepository).findByEmail(loginRequest.getEmail());
//         verify(securityContext).setAuthentication(authentication);
//         verify(session).setAttribute(eq("SPRING_SECURITY_CONTEXT"), any(SecurityContext.class));
//     }

//     @Test
//     void login_UserNotFound_ThrowsException() {
//         // Arrange
//         when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
//                 .thenReturn(authentication);
//         when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.empty());

//         SecurityContextHolder.setContext(securityContext);

//         // Act & Assert
//         assertThatThrownBy(() -> authService.login(loginRequest, session))
//                 .isInstanceOf(BusinessRuleViolationException.class)
//                 .hasMessage("User not found");

//         verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
//         verify(userRepository).findByEmail(loginRequest.getEmail());
//     }

//     @Test
//     void logout_Success() {
//         // Arrange
//         SecurityContextHolder.setContext(securityContext);

//         // Act
//         authService.logout(session);

//         // Assert
//         verify(session).invalidate();
//     }

//     @Test
//     void getCurrentUser_Authenticated_ReturnsUserDTO() {
//         // Arrange
//         when(authentication.isAuthenticated()).thenReturn(true);
//         when(authentication.getPrincipal()).thenReturn("test@example.com");
//         when(authentication.getName()).thenReturn("test@example.com");
//         when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

//         SecurityContextHolder.setContext(securityContext);
//         when(securityContext.getAuthentication()).thenReturn(authentication);

//         // Act
//         UserDTO result = authService.getCurrentUser();

//         // Assert
//         assertThat(result).isNotNull();
//         assertThat(result.getId()).isEqualTo(testUser.getId());
//         assertThat(result.getEmail()).isEqualTo(testUser.getEmail());
//         assertThat(result.getDisplayName()).isEqualTo(testUser.getDisplayName());
//         assertThat(result.getVerified()).isEqualTo(testUser.getVerified());
//         assertThat(result.getRoles()).isEqualTo(testUser.getRoles());
//         assertThat(result.getRiskScore()).isEqualTo(testUser.getRiskScore());
//         assertThat(result.getIsBlacklisted()).isEqualTo(testUser.getIsBlacklisted());

//         verify(userRepository).findByEmail("test@example.com");
//     }

//     @Test
//     void getCurrentUser_NotAuthenticated_ReturnsNull() {
//         // Arrange
//         SecurityContextHolder.setContext(securityContext);
//         when(securityContext.getAuthentication()).thenReturn(null);

//         // Act
//         UserDTO result = authService.getCurrentUser();

//         // Assert
//         assertThat(result).isNull();
//         verify(userRepository, never()).findByEmail(anyString());
//     }

//     @Test
//     void getCurrentUser_AnonymousUser_ReturnsNull() {
//         // Arrange
//         when(authentication.isAuthenticated()).thenReturn(true);
//         when(authentication.getPrincipal()).thenReturn("anonymousUser");

//         SecurityContextHolder.setContext(securityContext);
//         when(securityContext.getAuthentication()).thenReturn(authentication);

//         // Act
//         UserDTO result = authService.getCurrentUser();

//         // Assert
//         assertThat(result).isNull();
//         verify(userRepository, never()).findByEmail(anyString());
//     }

//     @Test
//     void getCurrentUser_UserNotInDatabase_ThrowsException() {
//         // Arrange
//         when(authentication.isAuthenticated()).thenReturn(true);
//         when(authentication.getPrincipal()).thenReturn("test@example.com");
//         when(authentication.getName()).thenReturn("test@example.com");
//         when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

//         SecurityContextHolder.setContext(securityContext);
//         when(securityContext.getAuthentication()).thenReturn(authentication);

//         // Act & Assert
//         assertThatThrownBy(() -> authService.getCurrentUser())
//                 .isInstanceOf(BusinessRuleViolationException.class)
//                 .hasMessage("User not found");

//         verify(userRepository).findByEmail("test@example.com");
//     }
// }
