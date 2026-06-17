package com.devpilot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.devpilot.entity.User;
import com.devpilot.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class InitialUserConfig {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public ApplicationRunner initialAdminUserInitializer(
            @Value("${app.admin.username}") String adminUsername,
            @Value("${app.admin.password}") String adminPassword) {
        return args -> initializeAdminUser(adminUsername, adminPassword);
    }

    private void initializeAdminUser(String adminUsername, String adminPassword) {
        if (!StringUtils.hasText(adminPassword)) {
            log.warn("管理员账号未初始化: username={}, reason=ADMIN_PASSWORD 未配置", adminUsername);
            return;
        }

        User admin = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, adminUsername));
        if (admin == null) {
            createAdminUser(adminUsername, adminPassword);
            return;
        }

        updateAdminUser(admin, adminPassword);
    }

    private void createAdminUser(String adminUsername, String adminPassword) {
        LocalDateTime now = LocalDateTime.now();
        User admin = new User();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setEnabled(1);
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        admin.setIsDeleted(0);

        userMapper.insert(admin);
        log.info("管理员账号已创建: userId={}, username={}", admin.getId(), adminUsername);
    }

    private void updateAdminUser(User admin, String adminPassword) {
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setRole("ADMIN");
        admin.setEnabled(1);
        admin.setUpdatedAt(LocalDateTime.now());

        userMapper.updateById(admin);
        log.info("管理员账号已同步: userId={}, username={}", admin.getId(), admin.getUsername());
    }
}
