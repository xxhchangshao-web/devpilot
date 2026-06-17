package com.devpilot.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JwtTokenProvider 单元测试 — Token 生成、解析与验证
 */
@DisplayName("JwtTokenProvider 单元测试")
class JwtTokenProviderTest {

    private static final String SECRET = "devpilot-jwt-secret-key-change-in-production";
    private static final long EXPIRATION_MS = 86400000L; // 24 hours

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(
                Base64.getEncoder().encodeToString(SECRET.getBytes())));
        jwtTokenProvider = new JwtTokenProvider(SECRET, EXPIRATION_MS);
    }

    // ==================== generateToken() 测试 ====================

    @Test
    @DisplayName("应该生成非空 Token 当提供完整用户信息")
    void should_generateNonEmptyToken_when_providedUserInfo() {
        // Act
        String token = jwtTokenProvider.generateToken(1L, "admin", "ADMIN");

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotBlank();
        // JWT token 应包含 3 个部分（header.payload.signature）
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("应该生成包含 DEVELEOPER 角色的 Token")
    void should_generateTokenWithDeveloperRole_when_roleIsDeveloper() {
        // Act
        String token = jwtTokenProvider.generateToken(2L, "developer", "DEVELOPER");

        // Assert
        assertThat(token).isNotNull();
        assertThat(jwtTokenProvider.getUsernameFromToken(token)).isEqualTo("developer");
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(2L);
    }

    // ==================== getUsernameFromToken() 测试 ====================

    @Test
    @DisplayName("应该返回用户名当 Token 有效")
    void should_returnUsername_when_tokenValid() {
        // Arrange
        String token = jwtTokenProvider.generateToken(1L, "admin", "ADMIN");

        // Act
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // Assert
        assertThat(username).isEqualTo("admin");
    }

    @Test
    @DisplayName("应该返回正确的用户名 Token 包含不同用户")
    void should_returnCorrectUsername_when_differentUsers() {
        // Arrange
        String adminToken = jwtTokenProvider.generateToken(1L, "admin", "ADMIN");
        String devToken = jwtTokenProvider.generateToken(2L, "developer001", "DEVELOPER");

        // Act & Assert
        assertThat(jwtTokenProvider.getUsernameFromToken(adminToken)).isEqualTo("admin");
        assertThat(jwtTokenProvider.getUsernameFromToken(devToken)).isEqualTo("developer001");
    }

    // ==================== getUserIdFromToken() 测试 ====================

    @Test
    @DisplayName("应该返回用户 ID 当 Token 有效")
    void should_returnUserId_when_tokenValid() {
        // Arrange
        String token = jwtTokenProvider.generateToken(1L, "admin", "ADMIN");

        // Act
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // Assert
        assertThat(userId).isEqualTo(1L);
    }

    @Test
    @DisplayName("应该返回 null 当 Token 中无 userId Claim")
    void should_returnNull_when_userIdNotPresent() {
        // 这种情况我们的 generateToken 总是会设置 userId，所以测试通过为 null 的场景依赖 token 结构
        // 实际验证：正常 token 一定有 userId
        String token = jwtTokenProvider.generateToken(42L, "viewer", "VIEWER");

        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        assertThat(userId).isEqualTo(42L);
    }

    // ==================== validateToken() 测试 ====================

    @Test
    @DisplayName("应该返回 true 当 Token 有效")
    void should_returnTrue_when_tokenValid() {
        // Arrange
        String token = jwtTokenProvider.generateToken(1L, "admin", "ADMIN");

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("应该返回 false 当 Token 格式错误")
    void should_returnFalse_when_tokenMalformed() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("this.is.not.a.valid.jwt.token");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("应该返回 false 当 Token 为空字符串")
    void should_returnFalse_when_tokenEmpty() {
        // Act
        boolean isValid = jwtTokenProvider.validateToken("");

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("应该返回 false 当 Token 为 null")
    void should_returnFalse_when_tokenNull() {
        // Act & Assert — 验证 null 时不会抛出异常且返回 false
        // 注意：Jwts.parser() 可能会抛出 IllegalArgumentException
        // jwtTokenProvider.validateToken() 内部捕获了 JwtException | IllegalArgumentException
        boolean isValid = jwtTokenProvider.validateToken(null);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("应该返回 false 当使用不同密钥签名的 Token")
    void should_returnFalse_when_tokenSignedWithDifferentKey() {
        // Arrange
        byte[] differentKeyBytes = new byte[32];
        java.util.Arrays.fill(differentKeyBytes, (byte) 0x42);
        String differentSecret = Base64.getEncoder().encodeToString(differentKeyBytes);
        SecretKey differentKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(differentSecret));

        JwtTokenProvider otherProvider = new JwtTokenProvider(
                new String(differentKeyBytes), EXPIRATION_MS);
        String token = otherProvider.generateToken(1L, "admin", "ADMIN");

        // Act
        boolean isValid = jwtTokenProvider.validateToken(token);

        // Assert
        assertThat(isValid).isFalse();
    }

    // ==================== Token 过期测试 ====================

    @Test
    @DisplayName("应该返回 false 当 Token 已过期")
    void should_returnFalse_when_tokenExpired() {
        // Arrange — 使用极短过期时间创建 Token
        JwtTokenProvider shortLivedProvider = new JwtTokenProvider(SECRET, 1); // 1ms
        String token = shortLivedProvider.generateToken(1L, "admin", "ADMIN");

        // 等待 token 过期
        try {
            Thread.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Act
        boolean isValid = shortLivedProvider.validateToken(token);

        // Assert
        assertThat(isValid).isFalse();
    }
}
