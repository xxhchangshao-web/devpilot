package com.devpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录成功响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    private String token;

    private String username;

    private String role;
}
