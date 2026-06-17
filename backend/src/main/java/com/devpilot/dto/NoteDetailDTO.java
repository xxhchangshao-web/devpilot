package com.devpilot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 笔记详情查询请求参数
 */
@Data
public class NoteDetailDTO {

    @NotNull(message = "笔记 ID 不能为空")
    @Min(value = 1, message = "笔记 ID 必须 >= 1")
    private Long id;
}
