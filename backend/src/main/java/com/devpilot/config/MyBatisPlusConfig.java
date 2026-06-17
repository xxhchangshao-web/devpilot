package com.devpilot.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.devpilot.mapper")
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 插件配置
     * <p>
     * 注册分页插件，支持 {@link com.baomidou.mybatisplus.extension.plugins.pagination.Page} 自动分页。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // PaginationInnerInterceptor 兼容 H2、PostgreSQL 等主流数据库
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.H2));
        return interceptor;
    }
}
