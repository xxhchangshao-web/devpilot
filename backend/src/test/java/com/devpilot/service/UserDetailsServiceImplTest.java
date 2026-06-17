package com.devpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devpilot.entity.User;
import com.devpilot.mapper.UserMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UserDetailsServiceImpl 单元测试 — 从数据库加载用户信息
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl 单元测试")
class UserDetailsServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    // ==================== loadUserByUsername() 测试 ====================

    @Test
    @DisplayName("应该返回 UserDetails 当用户存在且已启用")
    void should_returnUserDetails_when_userExistsAndEnabled() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPassword("$2a$10$encoded_password_hash");
        user.setRole("ADMIN");
        user.setEnabled(1);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("admin");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("$2a$10$encoded_password_hash");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_ADMIN");

        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("应该抛出 UsernameNotFoundException 当用户不存在")
    void should_throwUsernameNotFoundException_when_userNotFound() {
        // Arrange
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown_user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("应该抛出 UsernameNotFoundException 当账号被禁用 (enabled=0)")
    void should_throwUsernameNotFoundException_when_userDisabled() {
        // Arrange
        User user = new User();
        user.setId(2L);
        user.setUsername("disabled_user");
        user.setPassword("$2a$10$encoded_password_hash");
        user.setRole("DEVELOPER");
        user.setEnabled(0);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("disabled_user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("应该抛出 UsernameNotFoundException 当 enabled 为 null")
    void should_throwUsernameNotFoundException_when_enabledIsNull() {
        // Arrange
        User user = new User();
        user.setId(3L);
        user.setUsername("null_enabled_user");
        user.setPassword("$2a$10$encoded_password_hash");
        user.setRole("VIEWER");
        user.setEnabled(null);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("null_enabled_user"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户名或密码错误");

        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("应该返回 DEVELOPER 角色的 UserDetails")
    void should_returnUserDetailsWithDeveloperRole_when_userHasDeveloperRole() {
        // Arrange
        User user = new User();
        user.setId(2L);
        user.setUsername("developer");
        user.setPassword("$2a$10$dev_password_hash");
        user.setRole("DEVELOPER");
        user.setEnabled(1);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("developer");

        // Assert
        assertThat(result.getUsername()).isEqualTo("developer");
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_DEVELOPER");
        assertThat(result.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("应该返回 VIEWER 角色的 UserDetails")
    void should_returnUserDetailsWithViewerRole_when_userHasViewerRole() {
        // Arrange
        User user = new User();
        user.setId(3L);
        user.setUsername("viewer");
        user.setPassword("$2a$10$viewer_password_hash");
        user.setRole("VIEWER");
        user.setEnabled(1);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("viewer");

        // Assert
        assertThat(result.getUsername()).isEqualTo("viewer");
        assertThat(result.getAuthorities().iterator().next().getAuthority())
                .isEqualTo("ROLE_VIEWER");
        assertThat(result.isEnabled()).isTrue();
    }
}
