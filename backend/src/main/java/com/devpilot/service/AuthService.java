package com.devpilot.service;

import com.devpilot.dto.LoginParams;
import com.devpilot.dto.LoginResult;
import com.devpilot.security.JwtTokenProvider;
import com.devpilot.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

/**
 * 认证服务 — 处理登录和退出业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 用户登录认证
     * <p>
     * 委托 Spring Security AuthenticationManager 进行认证（BCrypt 密码比对），
     * 认证成功后生成 JWT Token 并返回用户信息。
     * 认证失败由 GlobalExceptionHandler 统一处理返回 401。
     *
     * @param params 登录请求参数（username, password）
     * @return 登录结果（token, username, role）
     */
    public LoginResult login(LoginParams params) {
        // 委托 Spring Security 进行认证（内含 BCrypt 密码比对）
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        params.getUsername(), params.getPassword()));

        SecurityUser securityUser = (SecurityUser) authentication.getPrincipal();

        // 生成包含 role 的 JWT Token
        String token = jwtTokenProvider.generateToken(
                securityUser.getUserId(),
                securityUser.getUsername(),
                securityUser.getRole());

        log.info("用户登录成功: {}", securityUser.getUsername());

        return new LoginResult(token, securityUser.getUsername(), securityUser.getRole());
    }

    /**
     * 退出登录
     * <p>
     * V1.0 无服务端状态变更（无状态 JWT），直接返回成功。
     * 前端负责清除 localStorage 中的 Token 并跳转登录页。
     */
    public void logout() {
        log.debug("用户退出登录");
    }
}
