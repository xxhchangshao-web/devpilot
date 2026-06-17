package com.devpilot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更改笔记状态请求参数
 */
@Data
public class ChangeStatusDTO {

    @NotNull(message = "笔记 ID 不能为空")
    private Long id;

    @NotBlank(message = "状态值不能为空")
    @Pattern(regexp = "^(OPEN|IN_PROGRESS|RESOLVED|ARCHIVED)$", message = "状态值无效，有效值：OPEN/IN_PROGRESS/RESOLVED/ARCHIVED")
    private String status;
}
