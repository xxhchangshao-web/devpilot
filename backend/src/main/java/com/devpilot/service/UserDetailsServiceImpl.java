package com.devpilot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devpilot.entity.User;
import com.devpilot.mapper.UserMapper;
import com.devpilot.security.SecurityUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Spring Security UserDetailsService 实现，从数据库加载用户信息
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, username)
        );

        if (user == null) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        // 账号被禁用时统一抛出 UsernameNotFoundException，
        // DaoAuthenticationProvider 会将其转为 BadCredentialsException，
        // 最终由 GlobalExceptionHandler 统一返回 "用户名或密码错误"，防止用户枚举攻击
        if (user.getEnabled() == null || user.getEnabled() != 1) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }

        return new SecurityUser(user);
    }
}
