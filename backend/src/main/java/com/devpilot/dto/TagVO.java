package com.devpilot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 标签视图对象（含关联笔记数统计，用于分页列表和创建/编辑响应）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagVO {

    private Long id;

    private String name;

    /**
     * 关联笔记数（通过 LEFT JOIN t_note_tag 统计）
     */
    private Integer noteCount;

    /**
     * 创建/更新时间（Jackson 按 application.yml 中 date-format 序列化）
     */
    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    /**
     * 用于构建简单标签（id + name）
     */
    public TagVO(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}
