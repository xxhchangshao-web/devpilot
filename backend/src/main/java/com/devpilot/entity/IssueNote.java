package com.devpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 问题笔记持久化对象，对应 t_issue_note 表
 */
@Data
@TableName("t_issue_note")
public class IssueNote {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题，1~200字符 */
    private String title;

    /** 问题描述 */
    private String description;

    /** 排查过程 */
    private String investigation;

    /** 解决方案 */
    private String solution;

    /** 状态：OPEN / IN_PROGRESS / RESOLVED / ARCHIVED */
    private String status;

    /** 优先级：P0 / P1 / P2 / P3 */
    private String priority;

    /** 分类：PERFORMANCE / EXCEPTION / CONFIG / ENVIRONMENT / BUSINESS_LOGIC / OTHER */
    private String category;

    /** 创建者ID，逻辑外键关联 t_user.id */
    private Long creatorId;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    /** 逻辑删除：0=正常，1=已删除 */
    @TableLogic
    private Integer isDeleted;
}
