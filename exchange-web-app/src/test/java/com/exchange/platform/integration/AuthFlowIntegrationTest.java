package com.exchange.platform.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Auth flow: register -> login -> me -> logout -> me(401)")
    void auth_flow_ok() throws Exception {
        String email = "user_" + UUID.randomUUID() + "@test.com";
        String password = "pw123";
        String displayName = "Tester";

        // register
        String regBody = String.format("{\"email\":\"%s\",\"password\":\"%s\",\"displayName\":\"%s\"}", email, password, displayName);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(regBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        // login and capture session cookie
        String loginBody = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", email, password);
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        // reuse the same HTTP session for subsequent requests
        var session = (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false);

        // me -> 200
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));

        // logout -> 204
        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent());

        // me -> 401
        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isUnauthorized());
    }
}
