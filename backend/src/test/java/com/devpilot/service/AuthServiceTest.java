package com.devpilot.service;

import com.devpilot.dto.LoginParams;
import com.devpilot.dto.LoginResult;
import com.devpilot.security.JwtTokenProvider;
import com.devpilot.security.SecurityUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AuthService 单元测试 — 登录和退出业务逻辑
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 单元测试")
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthService authService;

    // ==================== login() 测试 ====================

    @Test
    @DisplayName("应该返回登录结果当认证成功")
    void should_returnLoginResult_when_authenticationSuccessful() {
        // Arrange
        LoginParams params = new LoginParams();
        params.setUsername("admin");
        params.setPassword("admin123");

        SecurityUser securityUser = mock(SecurityUser.class);
        when(securityUser.getUserId()).thenReturn(1L);
        when(securityUser.getUsername()).thenReturn("admin");
        when(securityUser.getRole()).thenReturn("ADMIN");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtTokenProvider.generateToken(1L, "admin", "ADMIN"))
                .thenReturn("mock-jwt-token");

        // Act
        LoginResult result = authService.login(params);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getToken()).isEqualTo("mock-jwt-token");
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getRole()).isEqualTo("ADMIN");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateToken(1L, "admin", "ADMIN");
    }

    @Test
    @DisplayName("应该抛出 BadCredentialsException 当认证失败")
    void should_throwException_when_authenticationFails() {
        // Arrange
        LoginParams params = new LoginParams();
        params.setUsername("admin");
        params.setPassword("wrong_password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("用户名或密码错误"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(params))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("应该传递正确的认证参数给 AuthenticationManager")
    void should_passCorrectAuthenticationToken_when_login() {
        // Arrange
        LoginParams params = new LoginParams();
        params.setUsername("developer");
        params.setPassword("dev123456");

        SecurityUser securityUser = mock(SecurityUser.class);
        when(securityUser.getUserId()).thenReturn(2L);
        when(securityUser.getUsername()).thenReturn("developer");
        when(securityUser.getRole()).thenReturn("DEVELOPER");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(securityUser);
        when(jwtTokenProvider.generateToken(anyLong(), anyString(), anyString()))
                .thenReturn("token-for-developer");

        // Act
        LoginResult result = authService.login(params);

        // Assert
        assertThat(result.getUsername()).isEqualTo("developer");
        assertThat(result.getRole()).isEqualTo("DEVELOPER");
        assertThat(result.getToken()).isEqualTo("token-for-developer");
    }

    // ==================== logout() 测试 ====================

    @Test
    @DisplayName("应该成功完成退出登录（无状态 JWT，无服务端操作）")
    void should_completeSuccessfully_when_logout() {
        // Act
        authService.logout();

        // Assert — 无异常即通过
        verifyNoInteractions(authenticationManager, jwtTokenProvider);
    }
}
