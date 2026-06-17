package com.devpilot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 笔记分页查询请求参数
 */
@Data
public class NotePageQuery {

    /** 页码，默认 1 */
    @Min(value = 1, message = "页码必须 >= 1")
    private Integer pageNum = 1;

    /** 每页条数，默认 20 */
    @Min(value = 1, message = "每页条数必须 >= 1")
    @Max(value = 100, message = "每页条数必须在 1~100 之间")
    private Integer pageSize = 20;

    /** 关键词，模糊匹配 title + description */
    @Size(max = 200, message = "关键词不能超过 200 字符")
    private String keyword;

    /** 标签 ID 列表，AND 逻辑 */
    private List<Long> tagIds;

    /** 状态筛选 */
    @Pattern(regexp = "^(OPEN|IN_PROGRESS|RESOLVED|ARCHIVED)$", message = "状态值无效")
    private String status;

    /** 优先级筛选 */
    @Pattern(regexp = "^(P0|P1|P2|P3)$", message = "优先级值无效")
    private String priority;

    /** 分类筛选 */
    @Pattern(regexp = "^(PERFORMANCE|EXCEPTION|CONFIG|ENVIRONMENT|BUSINESS_LOGIC|OTHER)$", message = "分类值无效")
    private String category;
}
