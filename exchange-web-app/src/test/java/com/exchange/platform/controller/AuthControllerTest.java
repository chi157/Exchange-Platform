package com.exchange.platform.controller;

import com.exchange.platform.dto.AuthResponse;
import com.exchange.platform.dto.LoginRequest;
import com.exchange.platform.dto.RegisterRequest;
import com.exchange.platform.dto.UserDTO;
import com.exchange.platform.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

        @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/auth/register -> 201 when success")
    void register_success() throws Exception {
        Mockito.when(authService.register(any(RegisterRequest.class)))
                .thenReturn(AuthResponse.builder()
                        .success(true)
                        .message("Registration successful")
                        .userId(1L)
                        .email("u@test.com")
                        .displayName("U")
                        .build());

        String body = "{\"email\":\"u@test.com\",\"password\":\"p\",\"displayName\":\"U\"}";

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/auth/login -> 200 when ok; 401 when bad creds")
    void login_ok_and_unauthorized() throws Exception {
        Mockito.when(authService.login(any(LoginRequest.class), any()))
                .thenReturn(AuthResponse.builder().success(true).message("Login successful").build())
                .thenReturn(AuthResponse.builder().success(false).message("Invalid email or password").build());

        String ok = "{\"email\":\"u@test.com\",\"password\":\"p\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ok))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String bad = "{\"email\":\"u@test.com\",\"password\":\"wrong\"}";
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/auth/me -> 200 when session exists; 401 when not")
    void me_ok_and_unauthorized() throws Exception {
        Mockito.when(authService.getCurrentUser(any()))
                .thenReturn(UserDTO.builder().id(1L).email("u@test.com").displayName("U").verified(false).roles("USER").build())
                .thenReturn(null);

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u@test.com"));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/auth/logout -> 204")
    void logout_noContent() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent());
    }
}
