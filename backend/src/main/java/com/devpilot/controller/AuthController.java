package com.devpilot.controller;

import com.devpilot.common.ApiResponse;
import com.devpilot.dto.LoginParams;
import com.devpilot.dto.LoginResult;
import com.devpilot.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器 — 处理 /api/auth/** 请求
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * @param params 登录请求参数（含 @Valid 校验）
     * @return 登录成功后返回 JWT Token 和用户信息
     */
    @PostMapping("/login")
    public ApiResponse<LoginResult> login(@Valid @RequestBody LoginParams params) {
        LoginResult result = authService.login(params);
        return ApiResponse.success(result);
    }

    /**
     * 退出登录
     *
     * @return 成功响应（无数据返回）
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }
}
