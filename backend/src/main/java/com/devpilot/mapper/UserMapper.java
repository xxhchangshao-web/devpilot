package com.devpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.devpilot.entity.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper，基于 MyBatis-Plus 内置 CRUD
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
