package com.devpilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 笔记详情 VO，用于详情页展示
 */
@Data
@Builder
public class NoteDetailVO {

    /** 笔记 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 问题描述 */
    private String description;

    /** 排查过程 */
    private String investigation;

    /** 解决方案 */
    private String solution;

    /** 状态 */
    private String status;

    /** 优先级 */
    private String priority;

    /** 分类 */
    private String category;

    /** 关联标签列表 */
    private List<TagVO> tags;

    /** 创建者 ID */
    private Long creatorId;

    /** 创建者用户名 */
    private String creatorName;

    /** 创建时间 (yyyy-MM-dd HH:mm:ss) */
    @JsonProperty("created_at")
    private String createdAt;

    /** 更新时间 (yyyy-MM-dd HH:mm:ss) */
    @JsonProperty("updated_at")
    private String updatedAt;

    /** 当前用户是否可编辑（创建者或管理员为 true） */
    private Boolean canEdit;
}
