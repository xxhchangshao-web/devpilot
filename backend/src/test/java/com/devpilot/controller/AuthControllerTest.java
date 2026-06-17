package com.devpilot.controller;

import com.devpilot.dto.LoginParams;
import com.devpilot.dto.LoginResult;
import com.devpilot.security.JwtTokenProvider;
import com.devpilot.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 单元测试 — /api/auth/** 接口
 * <p>
 * 使用 @WebMvcTest 只加载 AuthController，不启动完整 Spring 上下文。
 * addFilters = false 跳过安全过滤器链，专注于 Controller 层行为验证。
 */
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController 单元测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    // ==================== POST /api/auth/login ====================

    @Nested
    @DisplayName("登录接口 POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("应该返回 200 和登录结果当凭据正确")
        void should_returnLoginResult_when_validCredentials() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername("admin");
            params.setPassword("admin123");

            LoginResult loginResult = new LoginResult("mock-jwt-token", "admin", "ADMIN");
            when(authService.login(any(LoginParams.class))).thenReturn(loginResult);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"))
                    .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                    .andExpect(jsonPath("$.data.username").value("admin"))
                    .andExpect(jsonPath("$.data.role").value("ADMIN"));

            verify(authService).login(any(LoginParams.class));
        }

        @Test
        @DisplayName("应该返回 400 当用户名为空")
        void should_return400_when_usernameBlank() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername("");
            params.setPassword("admin123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());

            verify(authService, org.mockito.Mockito.never()).login(any());
        }

        @Test
        @DisplayName("应该返回 400 当用户名为 null")
        void should_return400_when_usernameNull() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername(null);
            params.setPassword("admin123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当密码为空")
        void should_return400_when_passwordBlank() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername("admin");
            params.setPassword("");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当密码为 null")
        void should_return400_when_passwordNull() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername("admin");
            params.setPassword(null);

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当用户名长度不足")
        void should_return400_when_usernameTooShort() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername("ab");  // min=3
            params.setPassword("admin123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当密码长度不足")
        void should_return400_when_passwordTooShort() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername("admin");
            params.setPassword("12345");  // min=6

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("应该返回 400 当用户名包含非法字符")
        void should_return400_when_usernameHasInvalidChars() throws Exception {
            // Arrange
            LoginParams params = new LoginParams();
            params.setUsername("admin@test");  // @ not allowed
            params.setPassword("admin123");

            // Act & Assert
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(params)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== POST /api/auth/logout ====================

    @Nested
    @DisplayName("退出接口 POST /api/auth/logout")
    class LogoutTests {

        @Test
        @DisplayName("应该返回 200 当退出成功")
        void should_return200_when_logout() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/api/auth/logout")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.message").value("success"));

            verify(authService).logout();
        }
    }
}
