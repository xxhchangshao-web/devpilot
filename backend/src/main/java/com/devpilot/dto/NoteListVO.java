package com.devpilot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 笔记列表项 VO，用于分页列表展示
 */
@Data
@Builder
public class NoteListVO {

    /** 笔记 ID */
    private Long id;

    /** 标题 */
    private String title;

    /** 状态 */
    private String status;

    /** 优先级 */
    private String priority;

    /** 分类 */
    private String category;

    /** 关联标签列表 */
    private List<TagVO> tags;

    /** 创建者用户名 */
    private String creatorName;

    /** 创建时间 (yyyy-MM-dd HH:mm) */
    @JsonProperty("created_at")
    private String createdAt;

    /** 更新时间 (yyyy-MM-dd HH:mm) */
    @JsonProperty("updated_at")
    private String updatedAt;
}
