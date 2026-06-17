package com.devpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑笔记请求参数（不包含 status 字段，状态变更请使用 /api/notes/changeStatus）
 */
@Data
public class NoteUpdateDTO {

    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度必须在 1~200 字符之间")
    private String title;

    @NotBlank(message = "问题描述不能为空")
    @Size(max = 10000, message = "问题描述不能超过 10000 字符")
    private String description;

    /** 排查过程 */
    private String investigation;

    /** 解决方案 */
    private String solution;

    /** 优先级 */
    @Pattern(regexp = "^(P0|P1|P2|P3)$", message = "优先级值无效，有效值：P0/P1/P2/P3")
    private String priority;

    /** 分类 */
    @Pattern(regexp = "^(PERFORMANCE|EXCEPTION|CONFIG|ENVIRONMENT|BUSINESS_LOGIC|OTHER)$", message = "分类值无效")
    private String category;

    /** 关联标签 ID 列表 */
    private List<Long> tagIds;
}
